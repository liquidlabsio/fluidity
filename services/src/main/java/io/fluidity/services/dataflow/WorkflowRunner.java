package io.fluidity.services.dataflow;

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
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.*;

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
public abstract class WorkflowRunner {
    private final Logger log = LoggerFactory.getLogger(WorkflowRunner.class);


    public static final int N_THREADS = 20;
    private String tenant;
    private Storage storage;
    private String region;
    private String modelPath;

    private QueryService query;
    private final DataflowBuilder dfBuilder;
    private final ConcurrentLinkedQueue<String> statusQueue;
    private final ScheduledExecutorService scheduler;
    private String session;

    public WorkflowRunner(String tenant, String region, Storage storage, QueryService query, DataflowBuilder dfBuilder, String modelPath) {
        this.tenant = tenant;
        this.storage = storage;
        this.region = region;
        this.dfBuilder = dfBuilder;
        this.modelPath = modelPath;
        this.query = query;
        statusQueue = new ConcurrentLinkedQueue<>();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> drainStatusQueue(), 100, 100, TimeUnit.MILLISECONDS);
    }

    private void drainStatusQueue() {
        // write the queue to storage modelPath status file
        String msg = "";
        while ((msg = statusQueue.poll()) != null) {
            // write to storage
            log.info("====== Status:" + msg);
        }
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
            log.info(LogHelper.format(session, "builder", "workflow", "Files:" + filesToExtractFrom.length));

            // Stage 1. Rewrite dataflows by correlationId-timeFrom-timeTo =< fan-out
            stepOneExtractDataflowIntoStorage(search, filesToExtractFrom);

            DataflowHistoCollector dataflowHistoCollector = new DataflowHistoCollector(search);

            // Stage 2. Build DataFlows info for each correlation-set, collect histogram from all correlation-filenames
            buildDataFlowIndexForCorrelations(this.session, modelPath, dataflowHistoCollector);

            // Stage 3. Write it off to disk
            buildFinalModel(this.session, modelPath, dataflowHistoCollector, search.from, search.to);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.info(LogHelper.format(session, "builder", "workflow", "Failed:" + ex.toString()));
        } finally {
            log.info(LogHelper.format(session, "builder", "workflow", "Finish"));
        }
        drainStatusQueue();
        scheduler.shutdown();

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
        AtomicInteger modelsWritten = new AtomicInteger();
        try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(CORR_HIST_FMT, modelPath, start, end), 365)) {
            String dataflowHistogram = new ObjectMapper().writeValueAsString(dataflowHistoCollector.results());
            IOUtils.copy(new ByteArrayInputStream(dataflowHistogram.getBytes()), outputStream);
            modelsWritten.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            log.info(LogHelper.format(session, "builder", "buildFinalModel", "Failed:" + e.toString()));
        } finally {
            log.info(LogHelper.format(session, "builder", "buildFinalModel", "Finish modelsWritten:" + modelsWritten));
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

        List<String> currentCorrelationKeySet = new ArrayList<>();
        DataflowModeller dataflowModeller = new DataflowModeller();
        AtomicInteger foundCorrelations = new AtomicInteger();

        log.info(LogHelper.format(session, "builder", "buildCorrelations", "Start"));

        storage.listBucketAndProcess(region, tenant, modelPath + CORR_PREFIX, (region, itemUrl, correlationFilename) -> {
            String filenameonly = correlationFilename.substring(correlationFilename.lastIndexOf('/') + 1);
            String[] split = filenameonly.split(Model.DELIM);
            String correlationKey = split[1];
            String currentCorrelationKey = currentCorrelationKeySet.isEmpty() ? null : currentCorrelationKeySet.get(0);
            if (!correlationKey.equalsIgnoreCase(currentCorrelationKey) && currentCorrelationKey != null) {
                currentCorrelationKeySet.clear();
                currentCorrelationKeySet.add(correlationKey);
                foundCorrelations.incrementAndGet();
                writeCorrelationSet(session, modelPath, histoCollector, correlationSet, dataflowModeller, region, currentCorrelationKey);
                correlationSet.clear();
            }
            if (currentCorrelationKey == null) {
                currentCorrelationKeySet.add(correlationKey);
            }
            correlationSet.add(Pair.create(Long.parseLong(split[2]), correlationFilename));
            return null;
        });

        if (!currentCorrelationKeySet.isEmpty()) {
            String correlationKey = currentCorrelationKeySet.get(0);
            foundCorrelations.incrementAndGet();
            writeCorrelationSet(session, modelPath, histoCollector, correlationSet, dataflowModeller, region, correlationKey);
        }
        log.info(LogHelper.format(session, "builder", "buildCorrelations", "Finish, found:" + foundCorrelations));
        // flush the histo collector to storage
        statusQueue.add("Finished for Correlation Tasks");
    }

    private void writeCorrelationSet(String session, String modelPath, DataflowHistoCollector histoCollector, List<Pair<Long, String>> correlationSet, DataflowModeller dataflowModeller, String region, String correlationKey) {
        FlowInfo flow = dataflowModeller.getCorrelationFlow(correlationKey, correlationSet);
        histoCollector.add(flow.getStart(), flow);
        try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(CORR_FLOW_FMT, modelPath, correlationKey, flow.getStart(), flow.getEnd()), 365)) {
            String flowJson = new ObjectMapper().writeValueAsString(flow);
            IOUtils.copy(new ByteArrayInputStream(flowJson.getBytes()), outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage(), "Failed to process:{0}", correlationKey, e);
            log.info(LogHelper.format(session, "builder", "buildCorrelations", "Error:" + e.toString()));
        }
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
        Arrays.stream(submit).forEach(fileMeta -> pool.submit(() -> {
                    try {
                        statusQueue.add(rewriteCorrelationData(tenant, session, new FileMeta[]{fileMeta}, search, modelPath));
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
        ));
        try {
            Thread.sleep(1000);
            pool.shutdown();
            pool.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info(LogHelper.format(session, "workflow", "extractFlows", "Finish:" + search));
    }

    abstract String rewriteCorrelationData(String tenant, String session, FileMeta[] fileMeta, Search search, String modelPath);




}