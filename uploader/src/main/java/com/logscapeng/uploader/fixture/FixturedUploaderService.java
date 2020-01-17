package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FixturedUploaderService implements StorageUploader {
    private final Logger log = LoggerFactory.getLogger(FixturedUploaderService.class);

    static Map<String, byte[]> storage = new HashMap<>();

    public FixturedUploaderService(){
        log.info("CREATED");
    }

    @Override
    public FileMeta upload(FileMeta upload, String region) {
        log.info("uploading:" + upload);
        upload.setStorageUrl("s3://somebucket/"+region + "/" + upload.getFilename() + "-to-time=" + upload.getToTime());
        storage.put(upload.getStorageUrl(), upload.fileContent);
        upload.setFileContent(new byte[0]);
        return upload;
    }

    @Override
    public byte[] get(String storageUrl) {
        return storage.get(storageUrl);
    }
}
