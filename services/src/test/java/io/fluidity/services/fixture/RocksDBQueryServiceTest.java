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

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.server.RocksDBQueryService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class RocksDBQueryServiceTest {
    int tenantId;

    @Test
    void putGetStuff() {
        RocksDBQueryService db = new RocksDBQueryService();
        String tenant = tenantId++ + System.currentTimeMillis() + "";
        FileMeta fileMeta = new FileMeta(tenant, "server", "someTags", "thisMyFile.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        db.put(fileMeta);
        FileMeta fileMeta1 = db.find(fileMeta.tenant, fileMeta.filename);
        Assert.assertEquals(fileMeta, fileMeta1);
        List<FileMeta> list = db.list(tenant);
        Assert.assertEquals(fileMeta, list.get(0));

        db.delete(fileMeta.tenant, fileMeta.filename);
        List<FileMeta> list2 = db.list(tenant);
        Assert.assertTrue(list2.isEmpty());
    }

    @Test
    void batchOperations() {
        String tenant = tenantId++ + System.currentTimeMillis() + "";
        RocksDBQueryService db = new RocksDBQueryService();
        FileMeta fileMeta1 = new FileMeta(tenant, "server", "someTags", "thisMyFile1.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        FileMeta fileMeta2 = new FileMeta(tenant, "server", "someTags", "thisMyFile2.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");
        FileMeta fileMeta3 = new FileMeta(tenant, "server", "someTags", "thisMyFile3.log", "stuff".getBytes(), 0, System.currentTimeMillis(), "");

        db.putList(Arrays.asList(fileMeta1, fileMeta2, fileMeta3));

        List<FileMeta> list = db.list(tenant);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(fileMeta1.filename, list.get(0).filename);
    }
}