package io.precognito.services.search;

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
        return searchService.submit(search, query);
    }

    @POST
    @Path("/files/{tenant}/{files}/{mods}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] file(@PathParam("tenant") String tenant, @PathParam("files") String[] fileUrl, @PathParam("mods") Long[] mods, @MultipartForm Search search) {
        return searchService.searchFile(fileUrl, mods, search, storageService, cloudRegion, tenant);
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
