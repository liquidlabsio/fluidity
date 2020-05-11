package io.fluidity.services.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.*;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import org.apache.commons.io.IOUtils;
import org.graalvm.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static io.fluidity.dataflow.DataflowExtractor.CORR_PREFIX;

/**
 * Propagates through the stages of building the dataflow model
 * See: https://github.com/liquidlabsio/fluidity/issues/50
 * <p>
 * Stage 1.
 * Extract correlations into /bucket/modeldir/corr-{CORRELATIONID}-{TIMESTAMP}.log
 * <p>
 * Stage 2.
 * Scan model-dir for correlations, spawn tasks to build dataflow for each correlation - i.e. list files in flow order /bucket/modeldir/corr-{CORRELATINID}.index
 */
public class WorkflowRunnerV1 {
    private final Logger log = LoggerFactory.getLogger(WorkflowRunnerV1.class);


    public static final int N_THREADS = 20;
    private String tenant;
    private Storage storage;
    private String region;
    private String modelPath;

    private QueryService query;
    private final DataflowBuilder dfBuilder;
    private final ConcurrentLinkedQueue<Object> statusQueue;
    private final ScheduledExecutorService scheduler;
    private String session;
    private String apiUrl;

    public WorkflowRunnerV1(String tenant, DataflowBuilder dfBuilder, String modelPath, QueryService query, String apiUrl) {
        this.tenant = tenant;
        this.dfBuilder = dfBuilder;
        this.modelPath = modelPath;
        this.query = query;
        this.apiUrl = apiUrl;
        statusQueue = new ConcurrentLinkedQueue<>();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> drainStatusQueue(), 100, 100, TimeUnit.MILLISECONDS);
    }

    private void drainStatusQueue() {
        // write the queue to storage modelPath status file
        Object poll = statusQueue.poll();
        // write to storage
        log.info("Status:" + poll);
    }

    /**
     * @param search
     * @param session
     * @return
     */
    public String run(Search search, String session) {
        this.session = session;
        log.info(LogHelper.format(session, "builder", "workflow", "Start"));
        try {

            FileMeta[] filesToExtractFrom = dfBuilder.listFiles(search, query);

            // Stage 1. Rewrite dataflows by correlationId-timeFrom-timeTo =< fan-out
            stepOneExtractDataflowIntoStorage(search, filesToExtractFrom);

            DataflowHistoCollector dataflowHistoCollector = new DataflowHistoCollector(search);

            // Stage 2. Build DataFlows info for each correlation-set, collect histogram from all correlation-filenames
            buildDataFlowIndexForCorrelations(this.session, modelPath, dataflowHistoCollector);

            // Stage 3. Write it off to disk
            buildFinalModel(this.session, modelPath, dataflowHistoCollector, search.from, search.to);
        } catch (Exception ex) {
            log.info(LogHelper.format(session, "builder", "workflow", "Failed:" + ex.toString()));
        } finally {
            log.info(LogHelper.format(session, "builder", "workflow", "Finish"));
        }

        return this.session;
    }

    /**
     * an all index files to extract histogram analytics including:
     * number of dataflows
     * percentile breakdown of min,max,avg, 95th percentile, 99th percentile data
     *
     * @param modelPath
     * @param dataflowHistoCollector
     */
    private void buildFinalModel(String session, String modelPath, DataflowHistoCollector dataflowHistoCollector, long start, long end) {
        log.info(LogHelper.format(session, "builder", "buildFinalModel", "Start"));
        try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(DataflowExtractor.CORR_HIST_FMT, modelPath, start, end), 365)) {
            String dataflowHistogram = new ObjectMapper().writeValueAsString(dataflowHistoCollector);
            IOUtils.copy(new ByteArrayInputStream(dataflowHistogram.getBytes()), outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            log.info(LogHelper.format(session, "builder", "buildFinalModel", "Finish"));
        }
    }

    /**
     * Step 2.: For each dataflow generate span-information into a corr-time.flow file
     *
     * @param modelPath
     */
    private void buildDataFlowIndexForCorrelations(String session, String modelPath, DataflowHistoCollector histoCollector) {
        // storage.list files keeping track of each correlation
        List<Pair<Long, String>> correlationSet = new ArrayList<>();
        String currentCorrelationKey = "";

        log.info(LogHelper.format(session, "builder", "buildCorrelations", "Start"));

        storage.listBucketAndProcess(region, tenant, modelPath + CORR_PREFIX, (region, itemUrl, correlationFilename) -> {
            String[] split = correlationFilename.split("-");
            String correlationKey = split[1];
            if (!correlationKey.equalsIgnoreCase(currentCorrelationKey)) {
                DataflowModeller dataflowModeller = new DataflowModeller();
                FlowInfo flow = dataflowModeller.getCorrelationFlow(currentCorrelationKey, correlationSet);
                histoCollector.add(flow.getStart(), flow);
                try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(DataflowExtractor.CORR_FLOW_FMT, modelPath, currentCorrelationKey, flow.getStart(), flow.getEnd()), 365)) {
                    String flowJson = new ObjectMapper().writeValueAsString(flow);
                    IOUtils.copy(new ByteArrayInputStream(flowJson.getBytes()), outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            correlationSet.add(Pair.create(Long.parseLong(split[1]), correlationFilename));
            return null;
        });
        log.info(LogHelper.format(session, "builder", "buildCorrelations", "Finish"));
        // flush the histo collector to storage
        statusQueue.add("Finished for Correlation Tasks");
    }

    /**
     * Fan out to rewrite correlation information
     *
     * @param search
     * @param submit
     */
    private void stepOneExtractDataflowIntoStorage(Search search, FileMeta[] submit) {
        log.info(LogHelper.format(session, "workflow", "extractFlows", "Start:" + search));
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        Arrays.stream(submit).map(fileMeta -> pool.submit(() -> {
            String status;
            try {
                status = DataflowResource.rewriteCorrelationData(tenant, session, new FileMeta[]{fileMeta}, search, apiUrl, modelPath);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                status = "Failed:" + fileMeta + " " + e.toString();
            }
            statusQueue.add(status);
        }));
        try {
            pool.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info(LogHelper.format(session, "workflow", "extractFlows", "Finish:" + search));
    }
}
