package io.fluidity.search.agg.events;

import java.io.InputStream;

/**
 * Used to expose method to write to cloud storage
 */
public interface StorageUtil {
    void copyToStorage(InputStream currentFile, String region, String tenant, String filePath, int daysRetention, long lastModified);
}
