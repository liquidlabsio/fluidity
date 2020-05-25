/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.fixture;

import io.fluidity.search.StorageInputStream;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixturedStorageService implements Storage {
    private final Logger log = LoggerFactory.getLogger(FixturedStorageService.class);

    private Map<String, byte[]> storage = new HashMap<>();

    public FixturedStorageService() {
        log.info("Created");
    }

    @Override
    public FileMeta upload(String region, FileMeta upload) {
        log.info("uploading:" + upload);
        upload.setStorageUrl("storage://fixtured-storage-bucket/" + upload.getResource() + "/" + upload.getFilename());

        byte[] copy = new byte[upload.fileContent.length];
        System.arraycopy(upload.fileContent, 0, copy, 0, copy.length);
        storage.put(upload.getStorageUrl(), copy);
        return upload;
    }

    @Override
    public byte[] get(String region, String fileUrl, int offset) {
        byte[] bytes = storage.get(fileUrl);
        if (bytes == null) {
            throw new RuntimeException("Failed to load:" + fileUrl);
        }
        return Arrays.copyOfRange(bytes, offset, bytes.length);
    }

    @Override
    public List<FileMeta> importFromStorage(String cloudRegion, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags, String timeFormat) {
        return new ArrayList<>();
    }

    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        return "not implemented";
    }

    @Override
    public StorageInputStream getInputStream(String region, String tenant, String storageUrl) {
        byte[] content = this.get(region, storageUrl, 0);
        if (content == null)
            throw new RuntimeException(String.format("Failed to find:%s Available:%s", storageUrl, storage.keySet()));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        return new StorageInputStream(storageUrl, System.currentTimeMillis(), content.length, inputStream);
    }


    @Override
    public Map<String, StorageInputStream> getInputStreams(String region, String tenant, String uid, String filenameExtension, long fromTime) {
        List<String> fileUrls = storage.keySet().stream().filter(entry -> entry.contains(uid) && entry.endsWith(filenameExtension)).collect(Collectors.toList());
        LinkedHashMap<String, StorageInputStream> results = new LinkedHashMap<>();
        fileUrls.stream().forEach(url -> results.put(url, getInputStream(region, tenant, url)));
        return results;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void listBucketAndProcess(String region, String tenant, String prefix, Processor processor) {
        storage.entrySet().stream().filter(item -> item.getKey().contains(prefix)).forEach(item -> processor.process(region, item.getKey(), item.getKey(), System.currentTimeMillis()));
    }

    @Override
    public OutputStream getOutputStream(String region, String tenant, String fileUrl, int daysRetention) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                storage.put(fileUrl, this.buf);
            }
        };

        return baos;
    }

    @Override
    public String getBucketName(String tenant) {
        return "storage://" + tenant;
    }

    public Map<String, byte[]> getStorage() {
        return storage;
    }
}