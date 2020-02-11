package io.precognito.services.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;

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

    @ConfigProperty(name = "precognito.query")
    FileMetaDataQueryService query;

    @ConfigProperty(name = "precognito.search")
    SearchService searchService;

    @ConfigProperty(name = "precognito.storage")
    Storage storageService;

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return SearchResource.class.getCanonicalName();
    }

    @POST
    @Path("/submit")
    public FileMeta[] submit(Search search) {
        FileMeta[] submit = searchService.submit(search, query);
        return submit;
    }

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] file(@PathParam("tenant") String tenant, @PathParam("files") String fileMetas, @MultipartForm Search search) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            FileMeta[] fileMetas1 = objectMapper.readValue(URLDecoder.decode(fileMetas), FileMeta[].class);
            return searchService.searchFile(fileMetas1, search, storageService, cloudRegion, tenant);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/finalize/{tenant}/{histos}/{events}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] finaliseResults(@PathParam("tenant") String tenant, @PathParam("histos") String histos, @PathParam("events") String events,  @MultipartForm Search search) {

        long start = System.currentTimeMillis();
        try {

            String[] histoArray = histos.split(",");
            String[] eventsArray = events.split(",");

            return searchService.finalizeResults(Arrays.asList(histoArray), Arrays.asList(eventsArray), search, tenant, cloudRegion, storageService);
        } finally {
            log.info("Finalize Elapsed:{}",(System.currentTimeMillis() - start));
        }
    }
}
