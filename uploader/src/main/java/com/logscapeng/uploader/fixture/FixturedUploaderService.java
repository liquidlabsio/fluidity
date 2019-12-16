package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageUploader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixturedUploaderService implements StorageUploader {
    private final Logger log = LogManager.getLogger(FixturedUploaderService.class);

    public FixturedUploaderService(){
        log.warn("Created");
    }

    @Override
    public String upload(FileMeta upload, String region) {
        log.warn("uploading:" + upload);
        return getClass().getSimpleName() + " uploaded:" + upload;
    }
}
