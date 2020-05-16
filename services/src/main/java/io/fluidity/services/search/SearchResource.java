/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    QueryService query;

    SearchRunner searchRunner = new StandardSearchRunner();

    @ConfigProperty(name = "fluidity.services.storage")
    Storage storageService;

    @ConfigProperty(name = "fluidity.event.limit", defaultValue = "10000")
    int eventLimit;

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return SearchResource.class.getCanonicalName();
    }

    @POST
    @Path("/submit")
    public FileMeta[] submit(Search search) {
        try {
            log.info(LogHelper.format(search.uid, "search", "submit", "Start:" + search.expression));
            return searchRunner.submit(search, query);
        } finally {
            log.info(LogHelper.format(search.uid, "search", "submit", "End"));
        }
    }

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public List<String[]> file(@PathParam("tenant") String tenant, @PathParam("files") String fileMetas, @MultipartForm Search search) {
        try {
            search.decodeJsonFields();

            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] files = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.info("/search/file/{}", files.length);
            List<String[]> collect = Arrays.stream(files).map(fileMeta ->
                    searchRunner.searchFile(fileMeta, search, storageService, cloudRegion, tenant)
            ).collect(Collectors.toList());


            return collect;
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
            return searchRunner.finalizeEvents(search, from, eventLimit, tenant, cloudRegion, storageService);
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

            return searchRunner.finalizeHisto(search, tenant, cloudRegion, storageService);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
            log.info(LogHelper.format(search.uid, "search", "finalizeHisto", "End"));
        }
    }
}
