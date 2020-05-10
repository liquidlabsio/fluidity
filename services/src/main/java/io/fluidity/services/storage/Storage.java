package io.fluidity.services.storage;

import io.fluidity.services.Lifecycle;
import io.fluidity.services.query.FileMeta;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface Storage extends Lifecycle {
    /**
     * Store and Capture the Storage URL
     * @param region
     * @param upload
     * @return
     */
    FileMeta upload(String region, FileMeta upload);

    byte[] get(String region, String storageUrl, int offset);

    String getBucketName(String tenant);

    List<FileMeta> importFromStorage(String region, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags, String timeFormat);

    String getSignedDownloadURL(String region, String storageUrl);

    InputStream getInputStream(String region, String tenant, String storageUrl);

    OutputStream getOutputStream(String region, String tenant, String filePathUrl, int daysRetention);

    Map<String, InputStream> getInputStreams(String region, String tenant, List<String> urls);

    Map<String, InputStream> getInputStreams(String region, String tenant, String filePathPrefix, String filepathSuffix, long fromTime);

    void stop();

    /**
     * List in UTF-binary order
     *
     * @param region
     * @param tenant
     * @param prefix
     * @param processor
     */
    void listBucketAndProcess(String region, String tenant, String prefix, Processor processor);

    interface Processor {
        String process(String region, String itemUrl, String itemName);
    }
}
