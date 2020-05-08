package io.fluidity.services.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private List<Future<?>> collectCorrelationTasks;

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
        extractCorrelationDataIntoStorage(search, submit);

        // Stage 2.
        buildDataFlowIndexForCorrelations(modelPath);

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
     * Step 2.: For each dataflow generate span-information into a corr-time.index file
     *
     * @param modelPath
     */
    private void buildDataFlowIndexForCorrelations(String modelPath) {
        // storage.list files keeping track of each correlation
        EconomicMap<String, List<Pair<Long, String>>> correlationMap = EconomicMap.create();

        storage.listBucketAndProcess(region, tenant, modelPath + CORR_PREFIX, (region, itemUrl, itemName) -> {
            // expects filename to be corr-CORRELATIONID-TIMESTAMP1-TIMESTAMP2.log
            if (itemName.endsWith(".log")) {
                itemName = itemName.substring(0, itemName.indexOf(".log"));
                String[] split = itemName.split("-");
                String correlationKey = split[1];
                List<Pair<Long, String>> correlationSet = correlationMap.get(correlationKey);
                if (correlationSet == null) {
                    correlationMap.put(correlationKey, new ArrayList<>());
                    correlationSet = correlationMap.get(correlationKey);
                }
                correlationSet.add(Pair.create(Long.parseLong(split[1]), itemName));
            }
            return null;
        });
//        StreamSupport.stream(correlationMap.getValues().spliterator(), false).forEach(fileSet -> {
//            Collections.sort(fileSet, Comparator.comparing(Pair::getLeft));
//            // write the list to the modelDir
////            storage.getOutputStream()
////            fileSet.forEach(fileUrl -> {
////                try {
////                    baos.write(fileUrl.getRight().getBytes());
////                    baos.write('\n');
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//            });
//
//
//        });

        statusQueue.add("Finished for Correlation Tasks");
    }

    /**
     * Fan out to rewrite correlation information
     *
     * @param search
     * @param submit
     */
    private void extractCorrelationDataIntoStorage(Search search, FileMeta[] submit) {
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        collectCorrelationTasks = Arrays.stream(submit).map(fileMeta -> pool.submit(() -> {
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
