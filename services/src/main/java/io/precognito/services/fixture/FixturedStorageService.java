package io.precognito.services.fixture;

import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixturedStorageService implements Storage {
    private final Logger log = LoggerFactory.getLogger(FixturedStorageService.class);

    static Map<String, byte[]> storage = new HashMap<>();

    public FixturedStorageService(){
        log.info("Created");
    }

    @Override
    public FileMeta upload(String region, FileMeta upload) {
        log.info("uploading:" + upload);
        upload.setStorageUrl("s3://somebucket/"+region + "/" + upload.getFilename() + "-to-time=" + upload.getToTime());
        storage.put(upload.getStorageUrl(), upload.fileContent);
        upload.setFileContent(new byte[0]);
        return upload;
    }

    @Override
    public byte[] get(String region, String storageUrl) {
        return storage.get(storageUrl);
    }

    @Override
    public List<FileMeta> importFromStorage(String cloudRegion, String tenant, String storageId, String includeFileMask, String tags) {
        return new ArrayList<>();
    }

    @Override
    public List<FileMeta> removeByStorageId(String cloudRegion, String tenant, String storageId, String includeFileMask) {
        return new ArrayList<>();
    }

    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        return "not implemented";
    }
}
