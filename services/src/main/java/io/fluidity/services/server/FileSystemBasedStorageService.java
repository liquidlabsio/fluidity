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

package io.fluidity.services.server;

import io.fluidity.search.StorageInputStream;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.Storage;
import io.fluidity.util.DateUtil;
import io.fluidity.util.FileUtil;
import io.fluidity.util.LazyFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileSystemBasedStorageService implements Storage {
    public static final String FLUIDITY_FS_DIR = "fluidity.fs.dir";
    private String baseDir = System.getProperty(FLUIDITY_FS_DIR, "./data/fs");

    private final Logger log = LoggerFactory.getLogger(FileSystemBasedStorageService.class);

    public FileSystemBasedStorageService() {
        log.info("Created Working Dir:" + new File(".").getAbsolutePath());
        new File(baseDir).mkdirs();
        log.info("Using storage: {}", this.baseDir);
    }

    @Override
    public FileMeta upload(String region, FileMeta upload) {
        log.info("uploading:" + upload);

        String filenameAndPath = getFilename(upload.getResource(), upload.getFilename());
        try {
            FileUtil.writeFile(filenameAndPath, upload.fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to write {}", upload, e);
        }
        upload.setStorageUrl(filenameAndPath);
        return upload;
    }

    private String getFilename(String resource, String filename) {
        return String.format("%s/%s/%s", this.baseDir, resource, filename);
    }

    @Override
    public byte[] get(String region, String storageUrl, int offset) {
        if (storageUrl.startsWith("storage://")) storageUrl = storageUrl.substring("storage://".length());
        byte[] bytes = new byte[0];
        try {
            bytes = FileUtil.readFileToByteArray(new File(storageUrl), -1);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find file:" + storageUrl, e);
        }
        return Arrays.copyOfRange(bytes, offset, bytes.length);
    }

    @Override
    public List<FileMeta> importFromStorage(String cloudRegion, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags, String timeFormat) {

        log.info("Importing from:{} mask:{}", storageId, includeFileMask);
        String filePrefix = prefix.equals("*") ? "" : prefix;
        String fileMask = includeFileMask.equals("*") ? "" : includeFileMask;

        long since = System.currentTimeMillis() - ageDays * DateUtil.DAY;
        Collection<File> files = FileUtil.listDirs(storageId, "", filePrefix, fileMask);
        List<FileMeta> fileMetas = files.stream().filter(file -> file.lastModified() > since)
                .map(file ->
                {
                    String relativePath = file.getPath().startsWith(storageId) ? file.getPath().substring(storageId.length() + 1) : file.getPath();
                    FileMeta fm = new FileMeta(tenant, storageId, tags, relativePath, new byte[0], FileUtil.inferFakeStartTimeFromSize(file.length(), file.lastModified()), file.lastModified(), timeFormat);
                    fm.setSize(guessSize(file));
                    fm.setStorageUrl(file.getAbsolutePath());
                    return fm;
                })
                .collect(Collectors.toList());

        log.debug("Imported {}", fileMetas.size());
        return fileMetas;
    }

    private long guessSize(File file) {
        if (file.getName().endsWith(".gz")) return file.length() * 20l;
        if (file.getName().endsWith(".lz4")) return file.length() * 4l;
        return file.length();
    }

    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        return "not supported";
    }

    @Override
    public StorageInputStream getInputStream(String region, String tenant, String storageUrl) {
        try {
            File file = new File(storageUrl);
            return new StorageInputStream(file.getName(), file.lastModified(), file.length(), new LazyFileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, StorageInputStream> getInputStreams(String region, String tenant, String prefix, String filenameExtension, long fromTime) {
        Collection<File> files = FileUtil.listDirs(this.baseDir + "/" + prefix, filenameExtension);
        // Note: s3 is used as a storage prefix
        return files.stream().collect(Collectors.toMap(file -> "storage://" + file.getPath(), file -> {
            try {
                return new StorageInputStream(file.getName(), file.lastModified(), file.length(), new LazyFileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }));
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void listBucketAndProcess(String region, String tenant, String prefix, Processor processor) {
        List<File> files = new ArrayList(FileUtil.listDirs(this.baseDir + "/" + prefix, "*"));
        Collections.sort(new ArrayList(files), (Comparator<File>) (o1, o2) -> o1.getName().compareTo(o2.getName()));
        files.stream().forEach(item -> processor.process(region, FileUtil.fixPath(item.getPath()), FileUtil.fixPath(item.getPath()), item.lastModified(), item.length()));
    }

    @Override
    public OutputStream getOutputStream(String region, String tenant, String fullFilePath, int daysRetention, long lastModified) {
        if (fullFilePath.startsWith("storage://")) fullFilePath = fullFilePath.substring("storage://".length());
        if (fullFilePath.contains(baseDir)) {
            fullFilePath = fullFilePath.substring(fullFilePath.indexOf(baseDir) + baseDir.length());
        }
        File file = new File(this.baseDir, fullFilePath);
        file.setLastModified(lastModified);
        file.getParentFile().mkdirs();
        try {
            return new FileOutputStream(file) {
                @Override
                public void close() throws IOException {
                    super.close();
                    file.setLastModified(lastModified);
                }
            };
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBucketName(String tenant) {
        return String.format("%s", this.baseDir);
    }
}
