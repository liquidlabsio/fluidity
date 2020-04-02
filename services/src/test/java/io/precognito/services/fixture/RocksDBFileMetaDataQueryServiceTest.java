package io.precognito.services.fixture;

import io.precognito.services.query.FileMeta;
import io.precognito.services.server.RocksDBFileMetaDataQueryService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.precognito.services.server.RocksDBFileMetaDataQueryService.PRECOGNITO_FS_BASE_DIR;

class RocksDBFileMetaDataQueryServiceTest {

    @Test
    void putGetStuff() {
        System.setProperty(PRECOGNITO_FS_BASE_DIR, "./target/rocks-query" + System.currentTimeMillis());
        RocksDBFileMetaDataQueryService db = new RocksDBFileMetaDataQueryService();
        FileMeta fileMeta = new FileMeta("tenant", "server", "someTags", "thisMyFile.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        db.put(fileMeta);
        FileMeta fileMeta1 = db.find(fileMeta.tenant, fileMeta.filename);
        Assert.assertEquals(fileMeta, fileMeta1);
        List<FileMeta> list = db.list();
        Assert.assertEquals(fileMeta, list.get(0));

        db.delete(fileMeta.tenant, fileMeta.filename);
        List<FileMeta> list2 = db.list();
        Assert.assertTrue(list2.isEmpty());
    }

    @Test
    void batchOperations() throws IOException {

        System.setProperty(PRECOGNITO_FS_BASE_DIR, "./target/rocks-query-bulk" + System.currentTimeMillis());

        RocksDBFileMetaDataQueryService db = new RocksDBFileMetaDataQueryService();
        FileMeta fileMeta1 = new FileMeta("tenant", "server", "someTags", "thisMyFile1.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        FileMeta fileMeta2 = new FileMeta("tenant", "server", "someTags", "thisMyFile2.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        FileMeta fileMeta3 = new FileMeta("tenant", "server", "someTags", "thisMyFile3.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");

        db.putList(Arrays.asList(fileMeta1, fileMeta2, fileMeta3));
        List<FileMeta> list = db.list();
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(fileMeta1.filename, list.get(0).filename);
    }
}