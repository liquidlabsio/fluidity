package io.fluidity.services.search;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        log.info("/search/submit:{}", search);
        return searchRunner.submit(search, query);
    }

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] file(@PathParam("tenant") String tenant, @PathParam("files") String fileMetas, @MultipartForm Search search) {
        try {
            search.decodeJsonFields();

            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] fileMetas1 = objectMapper.readValue(URLDecoder.decode(fileMetas, StandardCharsets.UTF_8), FileMeta[].class);
            log.debug("/search/file/{}", fileMetas1[0].filename);
            return searchRunner.searchFile(fileMetas1, search, storageService, cloudRegion, tenant);
        } catch (Exception e) {
            log.error("/search/file:{} failed:{}", fileMetas, e.toString());
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/finalizeEvents/{tenant}/{fromTime}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] finaliseEvents(@PathParam("tenant") String tenant, @MultipartForm Search search, @PathParam("fromTime") long from) {

        try {

            log.info("/search/finalizeEvents:{}", search);
            search.decodeJsonFields();

            long start = System.currentTimeMillis();
            try {
                eventLimit = 10000;
                return searchRunner.finalizeEvents(search, from, eventLimit, tenant, cloudRegion, storageService);
            } finally {
                log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("finalizeEventsFailed", t);
            return new String[]{"0", "0", "0"};
        }
    }

    @POST
    @Path("/finalizeHisto/{tenant}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String finaliseHisto(@PathParam("tenant") String tenant, @MultipartForm Search search) {

        log.info("/search/finalizeHisto:{}", search);
        search.decodeJsonFields();

        long start = System.currentTimeMillis();
        try {

            return searchRunner.finalizeHisto(search, tenant, cloudRegion, storageService);
        } finally {
            log.info("Finalize Elapsed:{}", (System.currentTimeMillis() - start));
        }
    }
}
