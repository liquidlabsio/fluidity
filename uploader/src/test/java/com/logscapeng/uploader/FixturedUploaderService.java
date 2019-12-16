package com.logscapeng.uploader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixturedUploaderService implements StorageUploader {
    private final Logger log = LogManager.getLogger(FixturedUploaderService.class);

    @Override
    public String upload(FileMeta upload, String region) {
        log.warn("uploading:" + upload);
        return getClass().getSimpleName() + " uploaded:" + upload;
    }
}
