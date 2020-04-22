package io.fluidity.services.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.FileMetaDataQueryService;
import org.eclipse.microprofile.config.ConfigProvider;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RocksDBFileMetaDataQueryService implements FileMetaDataQueryService {
    public static final String PRECOGNITO_FS_BASE_DIR = "fluidity.rocks.base.dir";
    private static final String NAME = "Rocks_FileMetas";
    private final Logger log = LoggerFactory.getLogger(RocksDBFileMetaDataQueryService.class);

    private final String baseDir;
    private final File dbDir;
    private final ScheduledExecutorService scheduledThreadPool;
    private final RocksDB db;

    public RocksDBFileMetaDataQueryService() {
        log.info("Created");
        try {

            Optional<String> value = ConfigProvider.getConfig().getOptionalValue(PRECOGNITO_FS_BASE_DIR, String.class);
            if (value.isPresent()) {
                this.baseDir = value.get();
            } else {
                this.baseDir = "./target/storage/rocks-querystore";
            }
            RocksDB.loadLibrary();
            final Options options = new Options();
            options.setCreateIfMissing(true);
            dbDir = new File(baseDir, NAME);
            log.info("Using rocksDb: {}", this.baseDir);

            Files.createDirectories(dbDir.getParentFile().toPath());
            Files.createDirectories(dbDir.getAbsoluteFile().toPath());

            scheduledThreadPool = Executors.newScheduledThreadPool(1);

            db = RocksDB.open(options, dbDir.getAbsolutePath());

            scheduledThreadPool.scheduleWithFixedDelay(() -> {
                FlushOptions flushOptions = new FlushOptions();
                // flushOptions.setAllowWriteStall(true).setWaitForFlush(true);
                flushOptions.setWaitForFlush(true);
                try {
                    db.flush(flushOptions);
                } catch (RocksDBException e) {
                    e.printStackTrace();
                    log.error("Flush failed", e);
                }
            }, 1, 1, TimeUnit.MINUTES);
        } catch (IOException | RocksDBException ex) {
            ex.printStackTrace();
            log.error("Error initializng RocksDB, check configurations and permissions, exception: {}, message: {}, stackTrace: {}",
                    ex.getCause(), ex.getMessage(), ex.getStackTrace());
            throw new RuntimeException(ex);
        }
        log.debug("RocksDB initialized and ready to use");
    }

    @Override
    public void putList(List<FileMeta> fileMetas) {
        WriteOptions writeOptions = new WriteOptions();
        WriteBatch writeBatch = new WriteBatch();
        ObjectMapper objectMapper = getObjectMapper();
        fileMetas.forEach(item -> {
            try {
                writeBatch.put(item.filename.getBytes(), objectMapper.writeValueAsString(item).getBytes());
            } catch (RocksDBException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        try {
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            e.printStackTrace();
            log.error("Failed to write Batch: {}", fileMetas.size(), e);
        }
    }

    private ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void put(FileMeta fileMeta) {
        log.info("save");
        try {
            fileMeta.setFileContent(new byte[0]);
            db.put(fileMeta.filename.getBytes(), getObjectMapper().writeValueAsString(fileMeta).getBytes());
        } catch (RocksDBException | JsonProcessingException e) {
            log.error("Error saving entry in RocksDB, cause: {}, message: {}", e.getCause(), e.getMessage());
        }
    }

    @Override
    public FileMeta find(String tenant, String filename) {
        try {
            byte[] bytes = db.get(filename.getBytes());
            if (bytes == null) {
                log.warn("Failed to load file: {}", filename);
                return null;
            }
            return getObjectMapper().readValue(bytes, FileMeta.class);
        } catch (RocksDBException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load: " + filename);
        }
    }

    @Override
    public byte[] get(String tenant, String filename, int offset) {
        FileMeta fileMeta = find(tenant, filename);
        return fileMeta.getStorageUrl().getBytes();
    }

    @Override
    public void deleteList(List<FileMeta> removed) {
        removed.forEach(item -> delete(item.tenant, item.filename));
    }

    @Override
    public FileMeta delete(String tenant, String filename) {
        try {
            db.delete(filename.getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
            log.error("Failed to delete {}", filename);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<FileMeta> query(String tenant, String filenamePart, String tagNamePart) {

        List<FileMeta> results = new ArrayList<>();
        try {
            ObjectMapper objectMapper = getObjectMapper();

            RocksIterator iterator = db.newIterator();
            iterator.seekToFirst();
            while (iterator.isValid()) {
                String key = new String(iterator.key());
                if (key.contains(tenant) && key.contains(filenamePart)) {
                    try {
                        results.add(objectMapper.readValue(iterator.value(), FileMeta.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                iterator.next();
            }
            iterator.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Failed to query", ex);
        }
        return results;
    }

    @Override
    public List<FileMeta> list() {
        return query("", "", "");
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        log.info("Stopping");
        db.close();
    }
}
