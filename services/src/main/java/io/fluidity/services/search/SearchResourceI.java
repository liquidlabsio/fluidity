package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SearchResourceI {
    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    String id();

    @POST
    @Path("/submit/{tenant}")
    FileMeta[] submit(@PathParam("tenant") String tenant, Search search);

    @POST
    @Path("/files/{tenant}/{files}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    List<Integer[]> file(@PathParam("tenant") String tenant, @PathParam("files") String fileMetas, @MultipartForm Search search);

    @POST
    @Path("/finalizeEvents/{tenant}/{fromTime}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String[] finaliseEvents(@PathParam("tenant") String tenant, @MultipartForm Search search, @PathParam("fromTime") long from);

    @POST
    @Path("/finalizeHisto/{tenant}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String finaliseHisto(@PathParam("tenant") String tenant, @MultipartForm Search search);
}
