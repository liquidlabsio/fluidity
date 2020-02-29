package io.precognito.services.query;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;

/**
 * Captured relevant file meta data: name, location, source, size, tags etc
 */
@RegisterForReflection
public class FileMeta {


    public long getFromTime() {
            return fromTime;
        }

        public void setFromTime(long fromTime) {
            this.fromTime = fromTime;
        }

        public long getToTime() {
            return toTime;
        }

        public void setToTime(long toTime) {
            this.toTime = toTime;
        }

        public boolean isMatch(String filenamePart, String tagNamePart) {
            return (filenamePart.equals("*") || this.filename.contains(filenamePart)) && (tagNamePart.equals("*") || this.getTags().contains(tagNamePart));
        }

        // This is used to help with ORM mappings
        public enum Fields { filename, fileContent, tenant, resource, tags, storageUrl, fromTime, toTime, size}

        @FormParam("filename")
        @PartType(MediaType.TEXT_PLAIN)
        public String filename;

        @FormParam("fileContent")
        //    @PartType(MediaType.APPLICATION_OCTET_STREAM)
        @PartType(MediaType.TEXT_PLAIN)
        public byte[] fileContent;

        @FormParam("tenant")
        @PartType(MediaType.TEXT_PLAIN)
        public String tenant;

        @FormParam("resource")
        @PartType(MediaType.TEXT_PLAIN)
        public String resource;

        @FormParam("fromTime")
        @PartType(MediaType.TEXT_PLAIN)
        public long fromTime;

        @FormParam("toTime")
        @PartType(MediaType.TEXT_PLAIN)
        public long toTime;

        public long size;


        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        /**
         * Note: tags is a collection - but cannot be passed through a form
         * as such, so use a String and leave delimitation up to the user
         */
        @FormParam("tags")
        @PartType(MediaType.TEXT_PLAIN)
        public String tags;

    @FormParam("storageUrl")
    @PartType(MediaType.TEXT_PLAIN)
    public String storageUrl;


    public FileMeta() {
    }

    public FileMeta(String tenant, String resource, String tags, String filename, byte[] fileContent, long fromTime, long toTime) {
        this.tenant = tenant;
        this.resource = resource;
        this.tags = tags;
        this.filename = filename;
        this.fileContent = fileContent;
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    @Override
        public String toString() {
            return "FileMeta{" +
                    "tenant='" + tenant + '\'' +
                    ", resource='" + resource + '\'' +
                    ", tags='" + tags + '\'' +
                    ", filename='" + filename + '\'' +
                    //                ", filecontent=" + Arrays.toString(filecontent) +
                    '}';
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileMeta fileMeta = (FileMeta) o;

            if (!filename.equals(fileMeta.filename)) return false;
            if (!tenant.equals(fileMeta.tenant)) return false;
            return resource.equals(fileMeta.resource);
        }

        @Override
        public int hashCode() {
            int result = filename.hashCode();
            result = 31 * result + tenant.hashCode();
            result = 31 * result + resource.hashCode();
            return result;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public byte[] getFileContent() {
            return fileContent;
        }

        public void setFileContent(byte[] fileContent) {
            this.fileContent = fileContent;
        }

        public String getStorageUrl() {
            return storageUrl;
        }

        public void setStorageUrl(String storageUrl) {
            this.storageUrl = storageUrl;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getSize() {
            return size;
        }
}