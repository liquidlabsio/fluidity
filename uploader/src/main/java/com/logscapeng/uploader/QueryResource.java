package com.logscapeng.uploader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
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

    @Inject
    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-1")
    String cloudRegion;

    @ConfigProperty(name = "storage.query")
    FileMetaDataQueryService query;

    @ConfigProperty(name = "storage.uploader")
    StorageUploader uploader;


    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return QueryResource.class.getCanonicalName();
    }

    // TODO: no!!
    @Override
    public void createTable() {
    }

    @PUT
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
    @Path("/get")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] get(@QueryParam("tenant") String tenant, @QueryParam("filename")  String filename) {
        FileMeta fileMeta = query.find(tenant, filename);
        return uploader.get(fileMeta.getStorageUrl());
//        return fileMeta;
    }

    @DELETE
    @Path("/delete/{tenant}/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public FileMeta delete(@PathParam("tenant") String tenant, @PathParam("filename")  String filename) {
        return query.delete(tenant, filename);
    }

    @GET
    @Path("/query/{tenant}/{filenamePart}/{tagNamePart}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileMeta> query(
            @PathParam("tenant") String tenant
            , @PathParam("filenamePart") String filenamePart
            , @PathParam("tagNamePart") String tagNamePart
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
