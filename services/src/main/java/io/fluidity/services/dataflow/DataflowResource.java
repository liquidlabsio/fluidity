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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.LogHelper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API to build, maintain, and access dataflow models
 */

public class DataflowResource implements DataflowService {


    public static final String MODELS = "_MODELS_";
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
        ResteasyClient client = new ResteasyClientBuilderImpl().build();
        try {
            ResteasyWebTarget target = client.target(UriBuilder.fromPath(apiUrl));
            DataflowService proxy = target.proxy(DataflowService.class);
            return proxy.rewriteCorrelationData(tenant, sessionId, fileMetaJsonString, modelPath, search);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("BO0000OOM:" + ex.toString() + " URL:" + apiUrl);
            return ex.toString();
        } finally {
            client.close();
        }
    }

    @Override
    public List<Map<String, String>> model(String tenant, String session, String modelName) {
        modelName = modelPrefix + modelName;

        log.info("/model:{}", session);

        long start = System.currentTimeMillis();
        try {
            return dataflowBuilder.getModel(cloudRegion, tenant, session, modelName, storage);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
        }
    }

    @Override
    public String submit(String tenant, Search search, String modelName, String serviceAddress) {

        String sessionId = search.uid;
        modelName = modelPrefix + modelName;
        AtomicInteger rewritten = new AtomicInteger();
        log.info(LogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));
        WorkflowRunner runner = new WorkflowRunner(tenant, cloudRegion, storage, query, dataflowBuilder, modelName) {
            @Override
            String rewriteCorrelationData(String tenant, String session, FileMeta[] fileMeta, Search search, String modelPath) {
                try {
                    String result = rewriteCorrelationDataS(tenant, sessionId, fileMeta, search, serviceAddress, modelPath);
                    rewritten.incrementAndGet();
                    return result;
                } catch (JsonProcessingException e) {
                    log.info("Failed to invoke:" + serviceAddress + "/dataflow/rewrite");
                    e.printStackTrace();
                    return e.toString();
                }
            }
        };
        String userSession = runner.run(search, sessionId);
        log.info(LogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));

        try {
            return new ObjectMapper().writeValueAsString(userSession + " - rewritten:" + rewritten.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "{ \"msg\": \"failed\"}";
    }

    @Override
    public String rewriteCorrelationData(String tenant, String session, String fileMetas,
                                         String modelPath, Search search) {
        log.info(LogHelper.format(session, "workflow", "rewriteCorrelationData", "Start:" + fileMetas.length()));

        try {
            search.decodeJsonFields();
            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] files = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.info("/file/{}", files[0].filename);

            return dataflowBuilder.extractCorrelationData(session, files, search, storage, cloudRegion, tenant, modelPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("/search/file:{} failed:{}", fileMetas, e.toString());
            return "Failed:" + e.toString();
        } finally {
            log.info(LogHelper.format(session, "workflow", "rewriteCorrelationData", "End"));
        }
    }

    @Override
    public List<String> listModels(String tenant) {

        List<String> results = new ArrayList<>();
        storage.listBucketAndProcess(cloudRegion, tenant, MODELS, new Storage.Processor() {
            @Override
            public String process(String region, String itemUrl, String itemName, long modified) {
                results.add(itemName);
                return null;
            }
        });
        return results;
    }

    @Override
    public String loadModel(String tenant, String modelName) {
        String modelNameUrl = MODELS + "/" + modelName;
        return new String(storage.get(cloudRegion, modelNameUrl, 0));
    }

    @Override
    public String saveModel(String tenant, String modelName, String modelData) {
        String modelNameUrl = MODELS + "/" + modelName;

        try (OutputStream fos = storage.getOutputStream(cloudRegion, tenant, modelNameUrl, 360)) {
            fos.write(modelData.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to save:", modelName, e);
            return "Failed to save:" + e.toString();
        }
        return "Saved:" + modelData;
    }
}
