/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fluidity.dataflow.*;
import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import io.fluidity.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.graalvm.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.*;

/**
 * Propagates through the stages of building the dataflow model
 * See
 * - https://github.com/liquidlabsio/fluidity/issues/50
 * - https://github.com/liquidlabsio/fluidity/issues/62
 * - {@link io.fluidity.dataflow.Model}
 * <p>
 * Stage 1.
 * Extract correlations into /bucket/modeldir/corr-{CORRELATIONID}-{TIMESTAMP}.log
 * <p>
 * Stage 2.
 * Scan model-dir for correlations, spawn tasks to build dataflow for each correlation - i.e. list files in flow order /bucket/modeldir/corr-{CORRELATINID}.index
 *
 * Stage 3.
 * Store Histo and Ladder models for 50k and 20k foot visualizations
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
            log.info("====== QUEUE-Status:" + msg);
        }
    }

    /**
     * @param search
     * @param session
     * @return
     */
    public String run(Search search, String session) {
        this.session = session;
        log.info(FlowLogHelper.format(session, "builder", "workflow", "Start"));
        try {

            FileMeta[] filesToExtractFrom = dfBuilder.listFiles(tenant, search, query);
            log.info(FlowLogHelper.format(session, "builder", "workflow", "FileToCorrelate:" + filesToExtractFrom.length));

            // Stage 1. Rewrite dataflows by correlationId-timeFrom-timeTo =< fan-out
            stepOneExtractDataflowIntoStorage(search, filesToExtractFrom);

            DataflowHistoCollector dataflowHistoCollector = new DataflowHistoCollector(search);

            // Stage 2. Build DataFlow info for each correlation-set, collect histogram from all correlation-filenames
            buildDataFlowIndexForCorrelations(this.session, modelPath, dataflowHistoCollector);

            // Stage 3. Write it off to disk
            storeLadderModel(this.session, modelPath, dataflowHistoCollector, search.from, search.to);
            storeHistoModel(this.session, modelPath, dataflowHistoCollector, search.from, search.to);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.info(FlowLogHelper.format(session, "builder", "workflow", "Failed:" + ex.toString()));
        } finally {
            log.info(FlowLogHelper.format(session, "builder", "workflow", "Finish"));
        }
        drainStatusQueue();
        scheduler.shutdown();

        return this.session;
    }

    /**
     * Scan all flow files to extract histogram analytics including:
     * number of dataflows
     * percentile breakdown of min,max,avg, 95th percentile, 99th percentile data
     *
     * @param modelPath
     * @param dataflowHistoCollector
     */
    private void storeHistoModel(String session, String modelPath, DataflowHistoCollector dataflowHistoCollector, long start, long end) {
        log.info(FlowLogHelper.format(session, "builder", "storeHisto", "Start"));
        Collection<Series<FlowStats>> flowHisto = dataflowHistoCollector.flowHisto().slice(DateUtil.HOUR);
        ClientHistoJsonConvertor jsonConvertor = new ClientHistoJsonConvertor();
        flowHisto.forEach(item -> {
            try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(CORR_HIST_FMT, modelPath, item.start(), DateUtil.ceilHour(item.end())), 365, item.start())) {
                IOUtils.copy(new ByteArrayInputStream(jsonConvertor.toJson(item)), outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                log.info(FlowLogHelper.format(session, "builder", "storeHisto", "Failed:" + e.toString()));
            }});

        log.info(FlowLogHelper.format(session, "builder", "storeHisto", "Finish"));
    }
    // TODO: these ladders will get huge - revist breaking them down by timeframe
    private void storeLadderModel(String session, String modelPath, DataflowHistoCollector dataflowHistoCollector, long start, long end) {
        log.info(FlowLogHelper.format(session, "builder", "storeLadder", "Start"));
        Collection<Series<Map<Long, FlowStats>>> ladderHisto = dataflowHistoCollector.ladderHisto().slice(DateUtil.HOUR);
        ladderHisto.forEach(item -> {
            try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(LADDER_HIST_FMT, modelPath, item.start(), DateUtil.ceilHour(item.end())), 365, item.start())) {
                byte[] dataflowHistogram = getMapper().writeValueAsBytes(item);
                IOUtils.copy(new ByteArrayInputStream(dataflowHistogram), outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                log.info(FlowLogHelper.format(session, "builder", "storeLadder", "Failed:" + e.toString()));
            }
        });
        log.info(FlowLogHelper.format(session, "builder", "storeLadder", "Finish"));
    }

    /**
     * Step 2.: For each dataflow generate span-information into a corr-time.flow file
     *
     * @param modelPath
     */
    private void buildDataFlowIndexForCorrelations(String session, String modelPath, DataflowHistoCollector histoCollector) {
        // storage.list files keeping track of each correlation
        List<Pair<Long, String>> correlationFileSet = new ArrayList<>();

        List<String> currentCorrelationKeySet = new ArrayList<>();
        DataflowModeller dataflowModeller = new DataflowModeller();
        AtomicInteger foundCorrelations = new AtomicInteger();

        log.info(FlowLogHelper.format(session, "builder", "buildCorrelationFlows", "Start"));

        storage.listBucketAndProcess(region, tenant, modelPath, (region, itemUrl, correlationFilename, modified, size) -> {
            if (!correlationFilename.contains(CORR_PREFIX)) return;
            String filenameOnly = correlationFilename.substring(correlationFilename.lastIndexOf('/') + 1);
            String[] split = filenameOnly.split(Model.DELIM);
            String correlationKey = split[3];
            String currentCorrelationKey = currentCorrelationKeySet.isEmpty() ? null : currentCorrelationKeySet.get(0);
            if (!correlationKey.equalsIgnoreCase(currentCorrelationKey) && currentCorrelationKey != null) {
                currentCorrelationKeySet.clear();
                currentCorrelationKeySet.add(correlationKey);
                foundCorrelations.incrementAndGet();
                writeCorrelationFlow(session, modelPath, histoCollector, correlationFileSet, dataflowModeller, region, currentCorrelationKey);
                correlationFileSet.clear();
            }
            if (currentCorrelationKey == null) {
                currentCorrelationKeySet.add(correlationKey);
            }
            correlationFileSet.add(Pair.create(Long.parseLong(split[2]), correlationFilename));
        });

        if (!currentCorrelationKeySet.isEmpty()) {
            String correlationKey = currentCorrelationKeySet.get(0);
            foundCorrelations.incrementAndGet();
            writeCorrelationFlow(session, modelPath, histoCollector, correlationFileSet, dataflowModeller, region, correlationKey);
        }
        log.info(FlowLogHelper.format(session, "builder", "buildCorrelations", "Finish, found:" + foundCorrelations));
        // flush the histo collector to storage
        statusQueue.add("Finished for Correlation Tasks");
    }

    private void writeCorrelationFlow(String session, String modelPath, DataflowHistoCollector histoCollector, List<Pair<Long, String>> correlationFileSet, DataflowModeller dataflowModeller, String region, String correlationKey) {
        FlowInfo flow = dataflowModeller.getCorrelationFlow(correlationKey, correlationFileSet);
        histoCollector.add(flow.getStart(), flow);
        try (OutputStream outputStream = storage.getOutputStream(region, tenant, String.format(CORR_FLOW_FMT_2, modelPath, flow.getStart(), flow.getEnd(), correlationKey), 365, flow.getStart())) {
            String flowJson = getMapper().writeValueAsString(flow);
            IOUtils.copy(new ByteArrayInputStream(flowJson.getBytes()), outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), "Failed to process:{0}", correlationKey, e);
            log.info(FlowLogHelper.format(session, "builder", "buildCorrelations", "Error:" + e.toString()));
        }
    }

    /**
     * Fan out to extract correlation data from source files and write to storage
     *
     * @param search
     * @param submit
     */
    private void stepOneExtractDataflowIntoStorage(Search search, FileMeta[] submit) {
        log.info(FlowLogHelper.format(session, "workflow", "extractFlows", "Start:" + search));
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
        log.info(FlowLogHelper.format(session, "workflow", "extractFlows", "Finish:" + search));
    }

    abstract String rewriteCorrelationData(String tenant, String session, FileMeta[] fileMeta, Search search, String modelPath);


    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

}
