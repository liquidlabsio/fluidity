package io.precognito.services.fixture;

import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixturedStorageService implements Storage {
    private final Logger log = LoggerFactory.getLogger(FixturedStorageService.class);

    static Map<String, byte[]> storage = new HashMap<>();

    public FixturedStorageService(){
        log.info("Created");
    }

    @Override
    public FileMeta upload(String region, FileMeta upload) {
        log.info("uploading:" + upload);
        upload.setStorageUrl("s3://fixtured-storage-bucket/" + upload.getResource() + "/" + upload.getFilename());
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

    @Override
    public InputStream getInputStream(String region, String tenant, String storageUrl) {
        byte[] content = this.get(region, storageUrl);
        if (content == null) throw new RuntimeException(String.format("Failed to find:%s Available:%s", storageUrl, storage.keySet()));
        return new ByteArrayInputStream(content);
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, List<String> urls) {
        return urls.stream().collect(Collectors.toMap(url -> url, url -> getInputStream(region, tenant, url)));
    }

    @Override
    public OutputStream getOutputStream(String region, String tenant, String stagingFileResults) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                storage.put(stagingFileResults, this.buf);
            }
        };

        return baos;
    }

    @Override
    public String getBucketName(String tenant) {
        return "s3://" + tenant;
    }


}
