package com.logscapeng.uploader;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Captured relevant file meta data: name, location, source, size, tags etc
 */
public class FileMeta {

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

    public FileMeta(){};
    public FileMeta(String tenant, String resource, String[] tags, String filename, byte[] filecontent) {
        this.tenant = tenant;
        this.resource = resource;
        this.tags = Arrays.toString(tags);
        this.filename = filename;
        this.filecontent = filecontent;
    }

    public String getTenantWithDate() {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy");
        return tenant + "-" + sdf.format(today).toLowerCase();
    }

    @Override
    public String toString() {
        return "UploadMeta{" +
                "tenant='" + tenant + '\'' +
                ", resource='" + resource + '\'' +
                ", tags='" + tags + '\'' +
                ", filename='" + filename + '\'' +
                ", filecontent=" + Arrays.toString(filecontent) +
                '}';
    }
}
