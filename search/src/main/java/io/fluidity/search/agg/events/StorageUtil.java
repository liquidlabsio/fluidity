package io.fluidity.search.agg.events;

import java.io.File;

/**
 * Used to expose method to write to cloud storage
 */
public interface StorageUtil {
    void copyToStorage(File currentFile, String region, String tenant, String filePath, int daysRetention, long lastModified);
}
