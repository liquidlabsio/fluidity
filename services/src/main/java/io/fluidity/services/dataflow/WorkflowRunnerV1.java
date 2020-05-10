package io.fluidity.services.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.DataflowExtractor;
import io.fluidity.dataflow.DataflowHistoCollector;
import io.fluidity.dataflow.DataflowModeller;
import io.fluidity.dataflow.FlowInfo;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoFunction;
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
import java.util.stream.Collectors;

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

    private Search search;
    private String sessionId;

    private String apiUrl;

    private List<Future<?>> extractorTasks;

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
        log.info("Got Message:" + poll);
    }

    /**
     * @param search
     * @param userSession
     * @return
     */
    public String run(Search search, String userSession) {
        this.search = search;
        this.sessionId = userSession;

        FileMeta[] submit = dfBuilder.submit(search, query);

        // Stage 1. Rewrite dataflows by time-correlation (fan-out)
        stepOneExtractDataflowIntoStorage(search, submit);

        HistoFunction<Long, FlowInfo> histoFunction = new HistoFunction<>() {
            int count = 0;
            // for given index
            // emit client value
            StatsDuration totalDuration = new StatsDuration();
            StatsDuration op2OpLatency = new StatsDuration();
            StatsDuration maxOpDuration = new StatsDuration();

            @Override
            public Long calculate(FlowInfo currentValue, FlowInfo newValue, String nextLine, long position, long time, int histoIndex, String expression) {
                count++;
                long duration = newValue.getDuration();
                totalDuration.update(duration);

                long[] minMaxOpIntervalWithOpDuration = newValue.getMinOp2OpLatency();
                op2OpLatency.update(minMaxOpIntervalWithOpDuration[1]);

                maxOpDuration.update(minMaxOpIntervalWithOpDuration[2]);
                return 0l;
            }
        };
        DataflowHistoCollector dataflowHistoCollector = new DataflowHistoCollector(search, histoFunction);

        // Stage 2.
        buildDataFlowIndexForCorrelations(modelPath, dataflowHistoCollector);

        // Stage 3.
        buildFinalModel(modelPath);

        return sessionId;
    }

    /**
     * an all index files to extract histogram analytics including:
     * number of dataflows
     * percentile breakdown of min,max,avg, 95th percentile, 99th percentile data
     *
     * @param modelPath
     */
    private void buildFinalModel(String modelPath) {


    }

    /**
     * Step 2.: For each dataflow generate span-information into a corr-time.flow file
     *
     * @param modelPath
     */
    private void buildDataFlowIndexForCorrelations(String modelPath, DataflowHistoCollector histoCollector) {
        // storage.list files keeping track of each correlation
        List<Pair<Long, String>> correlationSet = new ArrayList<>();
        String currentCorrelationKey = "";


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
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        extractorTasks = Arrays.stream(submit).map(fileMeta -> pool.submit(() -> {
            String status;
            try {
                status = DataflowResource.rewriteCorrelationData(tenant, new FileMeta[]{fileMeta}, search, apiUrl, modelPath);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                status = "Failed:" + fileMeta + " " + e.toString();
            }
            statusQueue.add(status);
        })).collect(Collectors.toList());
        try {
            pool.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
