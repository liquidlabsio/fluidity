package io.precognito.services.storage;

import io.precognito.services.query.FileMeta;

import java.util.List;

public interface Storage {
    /**
     * Store and Capture the Storage URL
     * @param region
     * @param upload
     * @return
     */
    FileMeta upload(String region, FileMeta upload);

    byte[] get(String region, String storageUrl);

    List<FileMeta> importFromStorage(String cloudRegion, String tenant, String storageId, String includeFileMask, String tags);

    List<FileMeta> removeByStorageId(String cloudRegion, String tenant, String storageId, String includeFileMask);

    String getSignedDownloadURL(String region, String storageUrl);
}
