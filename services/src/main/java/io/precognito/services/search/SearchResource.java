package io.precognito.services.search;

import io.precognito.services.query.FileMetaDataQueryService;
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

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return SearchResource.class.getCanonicalName();
    }

    @POST
    @Path("/submit")
    public String[] submit(Search search) {
        return searchService.submit(search);
    }

    @POST
    @Path("/file/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] file(@PathParam("files") String[] fileUrl, @MultipartForm Search search) {
        return searchService.searchFile(fileUrl, search);
    }

    @POST
    @Path("/finalize/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String[] finaliseResults(@PathParam("files") String[] files, @MultipartForm Search search) {
        return searchService.finalizeResults(files, search);
    }
}
