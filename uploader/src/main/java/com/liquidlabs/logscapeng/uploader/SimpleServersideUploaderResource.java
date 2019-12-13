package com.liquidlabs.logscapeng.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.acme.quickstart.GreetingService;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * First (naive) implementation.
 * Server side uploader the runs with AWS Client credentials.
 * Loads directly to S3 bucket, driven by a REST based client that does a binary post.
 *
 * A Lambda wont handle the volume of data upload.
 */
@Path("/upload")
public class SimpleServersideUploaderResource {

    @Inject
    AWSS3UploaderService awss3UploaderService;


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return SimpleServersideUploaderResource.class.getCanonicalName();
    }

    @POST
    @Path("/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@MultipartForm UploadMeta uploadMeta) throws IOException {
        System.out.println("GOT FILe:" + uploadMeta);
        String upload = awss3UploaderService.upload(uploadMeta);
        return Response.status(200).entity(upload).build();
//        return Response.status(200).entity("Uploaded file name : " + uploadMeta.filename).build();
//        return null;
    }


    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {

            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                return name[1].trim().replaceAll("\"", "");
            }
        }
        return "unknown";
    }
}
