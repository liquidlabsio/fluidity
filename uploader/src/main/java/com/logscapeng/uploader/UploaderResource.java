package com.logscapeng.uploader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * First (naive) implementation.
 */
@Path("/upload")
public class UploaderResource {

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-1")
    String cloudRegion;

    @ConfigProperty(name = "storage.uploader")
    StorageUploader uploader;

    @ConfigProperty(name = "storage.indexer")
    StorageIndexer indexer;

    @ConfigProperty(name = "storage.query")
    FileMetaDataQueryService query;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return UploaderResource.class.getCanonicalName();
    }

    @POST
    @Path("/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@MultipartForm FileMeta fileMeta) {

        fileMeta.size = fileMeta.fileContent.length;

        // this series of actions should be put on an event queue
        FileMeta indexedFile = indexer.enrichMeta(fileMeta);
        FileMeta storedAndIndexedFile = uploader.upload(indexedFile, cloudRegion);
        // ideally we would trigger an indexing function from the S3 bucket write.
        // for now Im doing it in process here.
        FileMeta stored = indexer.index(storedAndIndexedFile, cloudRegion);


        query.put(stored);

        Response.ResponseBuilder responseBuilder = Response.status(200).entity("Uploaded and Indexed:" + stored);
        responseBuilder.header(  "Access-Control-Allow-Origin", "*");
        return responseBuilder.build();
    }
}
