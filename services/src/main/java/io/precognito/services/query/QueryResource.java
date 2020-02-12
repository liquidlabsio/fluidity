package io.precognito.services.query;


import io.precognito.services.storage.Storage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * First (naive) implementation.
 * Server side uploader the runs with AWS Client credentials.
 * Loads directly to S3 bucket, driven by a REST based client that does a multi-part, binary post.
 *
 */
@Path("/query")
public class QueryResource implements FileMetaDataQueryService {

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "precognito.services.query")
    FileMetaDataQueryService query;

    @ConfigProperty(name = "precognito.services.storage")
    Storage storage;

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return QueryResource.class.getCanonicalName();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void put(@MultipartForm FileMeta fileMeta) {
        fileMeta.setFileContent(new byte[0]);
        query.put(fileMeta);
    }

    @GET
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    public FileMeta find(@QueryParam("tenant") String tenant, @QueryParam("filename")  String filename) {
        return query.find(tenant, filename);
    }


    @GET
    @Path("/get/{tenant}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] get(@org.jboss.resteasy.annotations.jaxrs.PathParam("tenant") String tenant, @org.jboss.resteasy.annotations.jaxrs.PathParam("filename")  String filename) {
        FileMeta fileMeta = query.find(tenant, filename);
        if (fileMeta == null) {
            return ("Failed to find FileMeta for:" + tenant + " file:" + filename).getBytes();
        }
        return storage.get(cloudRegion, fileMeta.getStorageUrl());
    }

    @GET
    @Path("/getDownloadUrl/{tenant}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getDownloadUrl(@org.jboss.resteasy.annotations.jaxrs.PathParam("tenant") String tenant, @org.jboss.resteasy.annotations.jaxrs.PathParam("filename")  String filename) {
        FileMeta fileMeta = query.find(tenant, filename);
        if (fileMeta == null) {
            return "Error: Couldnt load FileMeta:" + tenant + "/"+ filename;
        }
        return storage.getSignedDownloadURL(cloudRegion, fileMeta.getStorageUrl());
    }

    /**
     * Note - using pathparams to enable GET request opening/downloading in browser tab
     * @param tenant
     * @param filename
     * @return
     */
    @GET
    @Path("/download/{tenant}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@org.jboss.resteasy.annotations.jaxrs.PathParam("tenant") String tenant, @org.jboss.resteasy.annotations.jaxrs.PathParam("filename")  String filename) {
        FileMeta fileMeta = query.find(tenant, filename);
        byte[] content = storage.get(cloudRegion, fileMeta.getStorageUrl());
        return Response.ok(content, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Length", content.length).build();
    }

    @DELETE
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    public FileMeta delete(@QueryParam("tenant") String tenant, @QueryParam("filename")  String filename) {
        return query.delete(tenant, filename);
    }

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileMeta> query(
            @QueryParam("tenant") String tenant
            , @QueryParam("filenamePart") String filenamePart
            , @QueryParam("tagNamePart") String tagNamePart
    ) {
        return query.query(tenant, filenamePart, tagNamePart);
    }

    @GET
    @Path("/list")
    @Produces("application/json")
    public List<FileMeta> list() {
        return query.list();
    }
}
