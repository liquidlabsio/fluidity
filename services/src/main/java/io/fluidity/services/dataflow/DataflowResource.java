/*
 *
 *
 * *
 *  * Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as
 *  * published by the Free Software Foundation, either version 3 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static io.fluidity.dataflow.Model.CORR_PREFIX;

/**
 * API to build, maintain and access dataflow models
 */
@Path("/dataflow")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataflowResource implements DataflowService {

    private final Logger log = LoggerFactory.getLogger(DataflowResource.class);

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "fluidity.services.query")
    QueryService query;

    DataflowBuilder dataflowBuilder = new DataflowBuilder();

    @ConfigProperty(name = "fluidity.services.storage")
    Storage storage;


    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return DataflowResource.class.getCanonicalName();
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
        ResteasyClient client = new ResteasyClientBuilderImpl().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(apiUrl));
        DataflowService proxy = target.proxy(DataflowService.class);
        return proxy.rewriteCorrelationData(tenant, sessionId, fileMetaString, modelPath, search);
    }

    @POST
    @Path("/submit/{tenant}/{serviceAddress}/{modelName}")
    @Override
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String submit(@PathParam("tenant") String tenant, @MultipartForm Search search, @PathParam("serviceAddress") String serviceAddress, @PathParam("modelName") String modelName) {

        String sessionId = search.uid;
        log.info(LogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));
        WorkflowRunner runner = new WorkflowRunner(tenant, cloudRegion, storage, query, dataflowBuilder, modelName) {
            @Override
            String rewriteCorrelationData(String tenant, String session, FileMeta[] fileMeta, Search search, String modelPath) {
                try {
                    return rewriteCorrelationDataS(tenant, sessionId, fileMeta, search, serviceAddress + "/dataflow/rewrite", modelPath);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return e.toString();
                }
            }
        };
        String userSession = runner.run(search, sessionId);
        log.info(LogHelper.format(sessionId, "dataflow", "submit", "Starting:" + search));
        return userSession;
    }

    /**
     * Returns status JSON for client to show process
     *
     * @param session
     * @return
     */
    @GET
    @Path("/status")
    @Override
    public String status(String tenant, String session, String modelName) {
        log.info("/status:{}", session);
        return dataflowBuilder.status(session, modelName);
    }

    @GET
    @Path("/model")
    @Override
    public String model(String tenant, String session, String modelName) {

        log.info("/model:{}", session);

        long start = System.currentTimeMillis();
        try {
            return dataflowBuilder.getModel(cloudRegion, tenant, session, modelName, storage);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
        }
    }

    @Override
    @POST
    @Path("/rewrite/{tenant}/{session}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String rewriteCorrelationData(@PathParam("tenant") String tenant, @PathParam("session") String session, @PathParam("files") String fileMetas, @PathParam("modelPath") String modelPath, @MultipartForm Search search) {
        search.decodeJsonFields();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] files = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.debug("/file/{}", files[0].filename);

            return dataflowBuilder.extractCorrelationData(session, files, search, storage, cloudRegion, tenant, modelPath + CORR_PREFIX);
        } catch (Exception e) {
            log.error("/search/file:{} failed:{}", fileMetas, e.toString());
            return "Failed:" + e.toString();
        }
    }

}
