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

package io.fluidity.services.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.LogHelper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *     submitSearch(search)
 *     searchFile(fileUrl, search)
 *     getFinalResult(searchId, searchedFiles)
 */
@Path("/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    private final Logger log = LoggerFactory.getLogger(SearchResource.class);

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "fluidity.services.query")
    public QueryService query;

    SearchRunner searchRunner = new StandardSearchRunner();

    @ConfigProperty(name = "fluidity.services.storage")
    public Storage storage;

    @ConfigProperty(name = "fluidity.event.limit", defaultValue = "10000")
    int eventLimit;

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return SearchResource.class.getCanonicalName();
    }

    @POST
    @Path("/submit/{tenant}")
    public FileMeta[] submit(@PathParam("tenant") String tenant, Search search) {
        try {
            log.info(LogHelper.format(search.uid, "search", "submit", "Start:" + search.expression));
            return searchRunner.submit(tenant, search, query);
        } finally {
            log.info(LogHelper.format(search.uid, "search", "submit", "End"));
        }
    }

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public List<Integer[]> file(@PathParam("tenant") String tenant, @PathParam("files") String fileMetas, @MultipartForm Search search) {
        try {
            search.decodeJsonFields();

            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] files = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.info("/search/file/{}", files.length);
            return searchRunner.searchFile(files, search, storage, cloudRegion, tenant);
        } catch (Exception e) {
            log.error("/search/file:{} failed:{}", fileMetas, e.toString());
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/finalizeEvents/{tenant}/{fromTime}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] finaliseEvents(@PathParam("tenant") String tenant, @MultipartForm Search search, @PathParam("fromTime") long from) {

        long start = System.currentTimeMillis();
        try {
            log.info(LogHelper.format(search.uid, "search", "finalizeEvents", "Start"));
            search.decodeJsonFields();
            eventLimit = 10000;
            return searchRunner.finalizeEvents(search, from, eventLimit, tenant, cloudRegion, storage);
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("finalizeEventsFailed", t);
            return new String[]{"0", "0", "0"};
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
            log.info(LogHelper.format(search.uid, "search", "finalizeEvents", "End"));
        }
    }

    @POST
    @Path("/finalizeHisto/{tenant}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String finaliseHisto(@PathParam("tenant") String tenant, @MultipartForm Search search) {

        long start = System.currentTimeMillis();
        try {
            log.info(LogHelper.format(search.uid, "search", "finalizeHisto", "Start"));
            search.decodeJsonFields();

            return searchRunner.finalizeHisto(search, tenant, cloudRegion, storage);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
            log.info(LogHelper.format(search.uid, "search", "finalizeHisto", "End"));
        }
    }
}
