package com.logscapeng.uploader;

public interface StorageUploader {
    String upload(FileMeta upload, String region);
}
