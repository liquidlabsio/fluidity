package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixturedUploaderService implements StorageUploader {
    private final Logger log = LoggerFactory.getLogger(FixturedUploaderService.class);

    public FixturedUploaderService(){
        log.info("CREATED");
    }

    @Override
    public FileMeta upload(FileMeta upload, String region) {
        log.info("uploading:" + upload);
        return upload;
    }
}
