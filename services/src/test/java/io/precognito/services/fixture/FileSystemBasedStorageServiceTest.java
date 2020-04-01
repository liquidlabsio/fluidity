package io.precognito.services.fixture;

import io.precognito.services.query.FileMeta;
import io.precognito.util.FileUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.precognito.services.fixture.RocksDBFileMetaDataQueryService.PRECOGNITO_FS_BASE_DIR;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileSystemBasedStorageServiceTest {

    @Test
    void uploadAndGet() {
        System.setProperty(FileSystemBasedStorageService.PRECOGNITO_FS_BASE_DIR, "target/test-data/storage" + System.currentTimeMillis());
        FileSystemBasedStorageService storage = new FileSystemBasedStorageService();
        byte[] fileContent = "Some bytes".getBytes();

        String filename = "stuff/" + System.currentTimeMillis() + "_myfile.log";
        FileMeta fileMetaData = new FileMeta("tenant", "server", "tags", filename, fileContent, 0, System.currentTimeMillis(), "");
        FileMeta uploaded = storage.upload("region", fileMetaData);

        assertNotNull(uploaded.getStorageUrl());

        byte[] bytes = storage.get("", uploaded.storageUrl, 0);
        Assert.assertNotNull(bytes);
    }

    @Test
    void bulkOperations() throws IOException {

        String testDir = "./target/test-data/storage/bulk" + System.currentTimeMillis();
        System.setProperty(PRECOGNITO_FS_BASE_DIR, testDir);

        FileUtil.writeFile(new File(System.getProperty(PRECOGNITO_FS_BASE_DIR), "file1.log").getPath(), "data1".getBytes());
        FileUtil.writeFile(new File(System.getProperty(PRECOGNITO_FS_BASE_DIR), "file2.log").getPath(), "data2".getBytes());
        FileUtil.writeFile(new File(System.getProperty(PRECOGNITO_FS_BASE_DIR), "file3.log").getPath(), "data2".getBytes());

        FileSystemBasedStorageService storage = new FileSystemBasedStorageService();

        List<FileMeta> fileMetas = storage.importFromStorage("region", "tenant", testDir, "", 30, "", "tags", "");

        Assert.assertEquals(3, fileMetas.size());
        Assert.assertEquals("tags", fileMetas.get(0).getTags());
    }
}