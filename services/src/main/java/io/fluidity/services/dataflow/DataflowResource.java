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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.ClientHistoJsonConvertor;
import io.fluidity.dataflow.FlowLogHelper;
import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.TimeSeries;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import io.vertx.core.TimeoutStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.CORR_HIST_PREFIX;
import static io.fluidity.dataflow.Model.LADDER_HIST_PREFIX;

/**
 * API to build, maintain, and access dataflow models
 */

public class DataflowResource implements DataflowService {


    public static final String MODELS = "_MODEL_";
    public static final char PATH_SEP = '/';
    private final Logger log = LoggerFactory.getLogger(DataflowResource.class);


    @ConfigProperty(name = "dataflow.prefix", defaultValue = "_MODEL_/")
    String modelPrefix;

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "fluidity.services.query")
    QueryService query;

    DataflowBuilder dataflowBuilder = new DataflowBuilder();

    @ConfigProperty(name = "fluidity.services.storage")
    Storage storage;


    public String id() {
        return DataflowResource.class.getCanonicalName();
    }

    /**
     * Returns status JSON for client to show process
     *
     * @param session
     * @return
     */
    @Override
    public String status(String tenant, String session, String modelName) {
        log.info("/status:{}", session);
        return dataflowBuilder.status(session, modelPrefix + modelName);
    }

    /**
     * Client call used to fan out on FaaS
     *
     * @param tenant
     * @param fileMetas
     * @param search
     * @param apiUrl
     * @return
     * @throws JsonProcessingException
     */
    public static String rewriteCorrelationDataS(String tenant, String sessionId, FileMeta[] fileMetas, Search search, String apiUrl, String modelPath) throws JsonProcessingException {
        String fileMetaString = new ObjectMapper().writeValueAsString(fileMetas);
        String fileMetaJsonString = URLEncoder.encode(fileMetaString, StandardCharsets.UTF_8);
        String modelPathJson = URLEncoder.encode(modelPath, StandardCharsets.UTF_8);

        ResteasyClient client = new ResteasyClientBuilderImpl().build();
        try {
            ResteasyWebTarget target = client.target(UriBuilder.fromPath(apiUrl));
            DataflowService proxy = target.proxy(DataflowService.class);
            return proxy.rewriteCorrelationData(tenant, sessionId, fileMetaJsonString, modelPathJson, search);
        } catch (Exception ex) {
            ex.printStackTrace();
            String failedMessage = "Failed to call onto REST API:" + ex.toString() + " URL:" + apiUrl + "Files:" + fileMetas[0];
            System.out.println(failedMessage);
            return failedMessage;
        } finally {
            client.close();
        }
    }

    @Override
    public List<Map<String, String>> modelDataList(String tenant, String session, String modelName) {
        modelName = modelPrefix + modelName;

        log.info("/model:{}", session);

        long start = System.currentTimeMillis();
        try {
            return dataflowBuilder.getModelDataList(cloudRegion, tenant, session, modelName, storage);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
        }
    }

    @Override
    public String submit(String tenant, Search search, String modelName, String serviceAddress) {

        String sessionId = search.uid;
        modelName = modelPrefix + modelName;
        AtomicInteger rewritten = new AtomicInteger();
        log.info(FlowLogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));
        WorkflowRunner runner = new WorkflowRunner(tenant, cloudRegion, storage, query, dataflowBuilder, modelName) {
            @Override
            String rewriteCorrelationData(String tenant, String session, FileMeta[] fileMeta, Search search, String modelPath) {
                try {
                    String result = rewriteCorrelationDataS(tenant, sessionId, fileMeta, search, serviceAddress, modelPath);
                    rewritten.incrementAndGet();
                    return result;
                } catch (JsonProcessingException e) {
                    log.warn("Failed to invoke:" + serviceAddress + "/dataflow/rewrite");
                    e.printStackTrace();
                    return "Failed to dispatch to REST:" + e.toString();
                }
            }
        };
        String userSession = runner.run(search, sessionId);
        log.info(FlowLogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));

        try {
            return new ObjectMapper().writeValueAsString(userSession + " - rewritten:" + rewritten.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "{ \"msg\": \"failed\"}";
    }

    @Override
    public String rewriteCorrelationData(String tenant, String session, String fileMetas,
                                         String modelPathEnc, Search search) {
        log.info(FlowLogHelper.format(session, "workflow", "rewriteCorrelationData", "Start:" + fileMetas.length()));

        try {
            search.decodeJsonFields();
            ObjectMapper objectMapper = new ObjectMapper();
            String modelPath = URLDecoder.decode(modelPathEnc, StandardCharsets.UTF_8);

            FileMeta[] files = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.info("/file/{}", files[0].filename);

            return dataflowBuilder.extractCorrelationData(session, files, search, storage, cloudRegion, tenant, modelPath);
        } catch (Exception e) {

            e.printStackTrace();
            log.error("/rewriteCorrelation:{} failed:{}", fileMetas, e.toString());
            return "Failed:" + e.toString();
        } finally {
            log.info(FlowLogHelper.format(session, "workflow", "rewriteCorrelationData", "End"));
        }
    }

    @Override
    public List<String> listModels(String tenant) {

        Set<String> results = new HashSet<>();
        storage.listBucketAndProcess(cloudRegion, tenant, MODELS, (region, itemUrl, itemName, modified, size) -> {
            int from = itemName.indexOf(MODELS) + MODELS.length() + 1;
            int to = itemName.indexOf(PATH_SEP, from);
            if (from > 0 && to > from) {
                results.add(itemName.substring(from, to));
            }
            return null;
        });
        return new ArrayList<>(results);
    }

    @Override
    public String loadModel(String tenant, String modelName) {
        String modelNameUrl = storage.getBucketName(tenant) + PATH_SEP + MODELS + PATH_SEP + modelName + "/model.json";
        try {
            byte[] bytes = storage.get(cloudRegion, modelNameUrl, 0);
            return new String(bytes);
        } catch (Throwable t) {
            // default to create a new model
            HashMap<Object, Object> model = new HashMap<>();
            model.put("name", modelName);
            model.put("query", "*|*|*|*");
            try {
                return new ObjectMapper().writeValueAsString(model);
            } catch (JsonProcessingException e) {
                log.error("Failed to load:" + e.toString());
                return e.toString();
            }
        }
    }

    @Override
    public String saveModel(String tenant, String modelName, String modelData) {
        String modelNameUrl = storage.getBucketName(tenant) + PATH_SEP + MODELS + PATH_SEP + modelName + "/model.json";

        try {
            try (OutputStream fos = storage.getOutputStream(cloudRegion, tenant, modelNameUrl, 360, System.currentTimeMillis())) {
                fos.write(modelData.getBytes());
            } catch (IOException e) {
                log.warn("Failed to save:", modelName, e);
                return new ObjectMapper().writeValueAsString("Failed to save:" + e.toString());
            }

            return new ObjectMapper().writeValueAsString(modelName);
        } catch (JsonProcessingException e) {
        }
        return "broken";
    }


    @Override
    public String volumeHisto(String tenant, String modelName, Long time) {

        modelName = modelPrefix + modelName;

        List<Map<String, String>> ladderAndHistoUrls = new ArrayList<>();
        storage.listBucketAndProcess(cloudRegion, tenant, modelName, (region1, itemUrl, itemName, modified, size) -> {
            if (itemUrl.contains(CORR_HIST_PREFIX)) {
                ladderAndHistoUrls.add(Map.of("url", itemUrl, "modified", Long.toString(modified), "data", Long.toString(size)));

            }
            return null;
        });
        ClientHistoJsonConvertor clientHistoJsonConvertor = new ClientHistoJsonConvertor();
        StringBuilder results = new StringBuilder();
        List<TimeSeries<FlowStats>> last = new ArrayList<>();
        ladderAndHistoUrls.stream().forEach(item -> {
            byte[] jsonPayload = storage.get(cloudRegion, item.get("url"), 0);
                TimeSeries<FlowStats> timeSeries = clientHistoJsonConvertor.fromJson(jsonPayload);
                last.add(timeSeries);
                if (timeSeries.start() < time && timeSeries.end() > time) {
                    results.append(clientHistoJsonConvertor.toClientArrays(timeSeries));
                }
        });
        if (results.length() == 0) {
            return clientHistoJsonConvertor.toClientArrays(last.get(last.size()-1));
        }
        return results.toString();
    }
}
