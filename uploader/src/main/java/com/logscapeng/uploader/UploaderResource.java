package com.logscapeng.uploader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * First (naive) implementation.
 * Server side uploader the runs with AWS Client credentials.
 * Loads directly to S3 bucket, driven by a REST based client that does a multi-part, binary post.
 *
 * A Lambda wont handle the volume of data required.
 */
@Path("/upload")
public class UploaderResource {

    @Inject
    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-1")
    String cloudRegion;

    @ConfigProperty(name = "storage.uploader")
    StorageUploader uploader;

    @ConfigProperty(name = "storage.indexer")
    StorageIndexer indexer;


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

        // this series of actions should be put on an event queue
        FileMeta indexedFile = indexer.enrichMeta(fileMeta);
        FileMeta storedAndIndexedFile = uploader.upload(indexedFile, cloudRegion);
        // ideally we would trigger an indexing function from the S3 bucket write.
        // for now Im doing it in process here.
        FileMeta stored = indexer.index(storedAndIndexedFile, cloudRegion);


        return Response.status(200).entity("Uploaded and Indexed:" + stored).build();
    }
}
