package com.logscapeng.uploader;

public interface StorageUploader {
    /**
     * Store and Capture the Storage URL
     * @param upload
     * @param region
     * @return
     */
    FileMeta upload(FileMeta upload, String region);
}
