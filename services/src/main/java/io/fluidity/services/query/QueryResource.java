package io.fluidity.services.query;


import io.fluidity.services.storage.Storage;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * First (naive) implementation.
 * Server side uploader the runs with AWS Client credentials.
 * Loads directly to S3 bucket, driven by a REST based client that does a multi-part, binary post.
 */
@Path("/query")
public class QueryResource implements FileMetaDataQueryService {

    private final Logger log = LoggerFactory.getLogger(QueryResource.class);


    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "fluidity.services.query")
    FileMetaDataQueryService query;

    @ConfigProperty(name = "fluidity.services.storage")
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
        log.debug("put");
        fileMeta.setFileContent(new byte[0]);
        query.put(fileMeta);
    }

    @Override
    public void putList(List<FileMeta> fileMetas) {
    }

    @GET
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    public FileMeta find(@QueryParam("tenant") String tenant, @QueryParam("filename") String filename) {
        log.debug("find");
        return query.find(tenant, filename);
    }


    @GET
    @Path("/get/{tenant}/{filename}/{offset}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] get(@PathParam("tenant") String tenant, @PathParam("filename") String filename, @PathParam("offset") int offset) {

        log.debug("get");
        // requested by storage URL
        if (filename.startsWith("s3://")) {
            return storage.get(cloudRegion, filename, offset);
        }
        // requested by filename
        FileMeta fileMeta = query.find(tenant, filename);
        if (fileMeta == null) {
            return ("Failed to find FileMeta for:" + tenant + " file:" + filename).getBytes();
        }
        byte[] bytes = storage.get(cloudRegion, fileMeta.getStorageUrl(), offset);
        if (fileMeta.filename.endsWith(".lz4")) {
            LZ4FrameInputStream inStream = null;
            try {
                inStream = new LZ4FrameInputStream(new ByteArrayInputStream(bytes));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                inStream.transferTo(baos);
                return baos.toByteArray();
            } catch (IOException e) {
                log.error("Failed to decode lz4:{}", filename, e);
                return "Could not decode LZ4 data".getBytes();
            }
        }
        return bytes;
    }

    @GET
    @Path("/getDownloadUrl/{tenant}/{filename}/{offset}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getDownloadUrl(@PathParam("tenant") String tenant, @PathParam("filename") String filename, @PathParam("offset") int offset) {
        FileMeta fileMeta = query.find(tenant, filename);
        if (fileMeta == null) {
            return "Error: Couldnt load FileMeta:" + tenant + "/" + filename;
        }
        return storage.getSignedDownloadURL(cloudRegion, fileMeta.getStorageUrl());
    }

    /**
     * Note - using pathparams to enable GET request opening/downloading in browser tab
     *
     * @param tenant
     * @param filename
     * @return
     */
    @GET
    @Path("/download/{tenant}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("tenant") String tenant, @PathParam("filename") String filename) {
        log.debug("download");
        FileMeta fileMeta = query.find(tenant, filename);
        byte[] content = storage.get(cloudRegion, fileMeta.getStorageUrl(), 0);
        return Response.ok(content, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Length", content.length).build();
    }

    @DELETE
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    public FileMeta delete(@QueryParam("tenant") String tenant, @QueryParam("filename")  String filename) {
        log.debug("delete");
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
        log.debug("query");
        return query.query(tenant, filenamePart, tagNamePart);
    }

    @GET
    @Path("/list")
    @Produces("application/json")
    public List<FileMeta> list() {

        log.debug("list");
        return query.list();
    }

    @Override
    public void deleteList(List<FileMeta> removed) {

    }
}
