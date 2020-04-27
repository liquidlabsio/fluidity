package io.fluidity.services.server;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.Storage;
import io.fluidity.util.DateUtil;
import io.fluidity.util.FileUtil;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileSystemBasedStorageService implements Storage {
    public static final String PRECOGNITO_FS_BASE_DIR = "fluidity.fs.base.dir";
    private final Logger log = LoggerFactory.getLogger(FileSystemBasedStorageService.class);

    private final String baseDir;

    public FileSystemBasedStorageService() {
        log.info("Created Working Dir");
        Optional<String> value = ConfigProvider.getConfig().getOptionalValue(PRECOGNITO_FS_BASE_DIR, String.class);
        if (value.isPresent()) {
            this.baseDir = value.get();
        } else {
            this.baseDir = "./storage/fs";
        }
        new File(baseDir).mkdirs();
        log.info("Using storage: {}", this.baseDir);
    }

    @Override
    public FileMeta upload(String region, FileMeta upload) {
        log.info("uploading:" + upload);

        String filenameAndPath = getFilename(upload.getTenant(), upload.getResource(), upload.getFilename());
        try {
            FileUtil.writeFile(filenameAndPath, upload.fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to write {}", upload, e);
        }
        upload.setStorageUrl(filenameAndPath);
        return upload;
    }

    private String getFilename(String tenant, String resource, String filename) {
        return String.format("%s/%s/%s", this.baseDir, resource, filename);
    }

    @Override
    public byte[] get(String region, String storageUrl, int offset) {
        if (storageUrl.startsWith("s3://")) storageUrl = storageUrl.substring("s3://".length());
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
                    FileMeta fm = new FileMeta(tenant, storageId, tags, relativePath, new byte[0], inferFakeStartTimeFromSize(file.length(), file.lastModified()), file.lastModified(), timeFormat);
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

    private long inferFakeStartTimeFromSize(long size, long lastModified) {
        // some file systems can return '0'
        if (size < 4096) return lastModified - DateUtil.HOUR;
        int fudgeLineLength = 256;
        int fudgeLineCount = (int) (size / fudgeLineLength);
        long fudgedTimeIntervalPerLineMs = 1000;
        long startTimeOffset = fudgedTimeIntervalPerLineMs * fudgeLineCount;
        if (startTimeOffset < DateUtil.HOUR) startTimeOffset = DateUtil.HOUR;
        return lastModified - startTimeOffset;
    }

    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        return "not supported";
    }

    @Override
    public InputStream getInputStream(String region, String tenant, String storageUrl) {
        try {
            return new FileInputStream(storageUrl);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, List<String> urls) {
        return urls.stream().collect(Collectors.toMap(url -> url, url -> getInputStream(region, tenant, url)));
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, String uid, String filenameExtension, long fromTime) {
        Collection<File> files = FileUtil.listDirs(this.baseDir, filenameExtension, tenant, uid);
        // Note: s3 is used as a storage prefix
        return files.stream().collect(Collectors.toMap(file -> "s3://" + file.getPath(), file -> {
            try {
                return new FileInputStream(file);
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
    public OutputStream getOutputStream(String region, String tenant, String stagingFileResults, int daysRetention) {
        if (stagingFileResults.startsWith("s3://")) stagingFileResults = stagingFileResults.substring("s3://".length());
        File file = new File(stagingFileResults);
        file.getParentFile().mkdirs();
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBucketName(String tenant) {
        return String.format("%s/%s", this.baseDir, tenant);
    }
}
