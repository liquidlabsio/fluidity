package io.precognito.services.storage;

import io.precognito.services.query.FileMeta;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface Storage {
    /**
     * Store and Capture the Storage URL
     * @param region
     * @param upload
     * @return
     */
    FileMeta upload(String region, FileMeta upload);

    byte[] get(String region, String storageUrl);

    String getBucketName(String tenant);

    List<FileMeta> importFromStorage(String region, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags);

    String getSignedDownloadURL(String region, String storageUrl);

    InputStream getInputStream(String region, String tenant, String storageUrl);

    OutputStream getOutputStream(String region, String tenant, String stagingFileResultsUrl);

    Map<String, InputStream> getInputStreams(String region, String tenant, List<String> urls);

    Map<String, InputStream> getInputStreams(String region, String tenant, String filePathPrefix, String filepathSuffix, long fromTime);
}
