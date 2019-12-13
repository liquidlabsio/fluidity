package com.liquidlabs.logscapeng.uploader;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
//import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;

public class UploadMeta {

    @FormParam("tenant")
    @PartType(MediaType.TEXT_PLAIN)
    public String tenant;

    @FormParam("resource")
    @PartType(MediaType.TEXT_PLAIN)
    public String resource;

    @FormParam("tags")
    @PartType(MediaType.TEXT_PLAIN)
    public String tags;

    @FormParam("filename")
    @PartType(MediaType.TEXT_PLAIN)
    public String filename;

    @FormParam("filecontent")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] filecontent;

    public UploadMeta(){};
    public UploadMeta(String tenant, String resource, String[] tags, String filename, byte[] filecontent) {
        this.tenant = tenant;
        this.resource = resource;
        this.tags = Arrays.toString(tags);
        this.filename = filename;
        this.filecontent = filecontent;
    }
}
