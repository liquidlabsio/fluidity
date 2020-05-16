/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.services.fixture;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        upload.setStorageUrl("s3://fixtured-storage-bucket/" + upload.getResource() + "/" + upload.getFilename());

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
    public InputStream getInputStream(String region, String tenant, String storageUrl) {
        byte[] content = this.get(region, storageUrl, 0);
        if (content == null)
            throw new RuntimeException(String.format("Failed to find:%s Available:%s", storageUrl, storage.keySet()));
        return new ByteArrayInputStream(content);
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, List<String> urls) {
        // Note: Using linked hashmap to to match the urls list indexing
        LinkedHashMap<String, InputStream> results = new LinkedHashMap<>();
        urls.stream().forEach(url -> results.put(url, getInputStream(region, tenant, url)));
        return results;
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, String uid, String filenameExtension, long fromTime) {
        List<String> fileUrls = storage.keySet().stream().filter(entry -> entry.contains(uid) && entry.endsWith(filenameExtension)).collect(Collectors.toList());
        return getInputStreams(region, tenant, fileUrls);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void listBucketAndProcess(String region, String tenant, String prefix, Processor processor) {
        storage.entrySet().stream().filter(item -> item.getKey().contains(prefix)).forEach(item -> processor.process(region, item.getKey(), item.getKey()));
    }

    @Override
    public OutputStream getOutputStream(String region, String tenant, String stagingFileResults, int daysRetention) {
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

    public Map<String, byte[]> getStorage() {
        return storage;
    }
}