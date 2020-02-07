package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 *     submitSearch(search)
 *     searchFile(fileUrl, search)
 *     getFinalResult(searchId, searchedFiles)
 */
@Path("/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

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
    public String[] submit(Search search) {
        return searchService.submit(search, query);
    }

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] file(@PathParam("tenant") String tenant, @PathParam("files") String[] fileUrl, @MultipartForm Search search) {
        return searchService.searchFile(fileUrl, search, storageService, cloudRegion, tenant);
    }

    @POST
    @Path("/finalize/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] finaliseResults(@PathParam("tenant") String tenant, @PathParam("files") String[] files, @MultipartForm Search search) {
        return searchService.finalizeResults(files, search);
    }
}
