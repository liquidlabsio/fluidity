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

package io.fluidity.services.dataflow;

import io.fluidity.dataflow.FlowLogHelper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.server.FileSystemBasedStorageService;
import io.fluidity.services.server.RocksDBQueryService;
import io.fluidity.services.storage.Storage;
import io.fluidity.test.IntegrationTest;
import io.fluidity.util.DateUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.CORR_HIST_PREFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@IntegrationTest
class WorkflowRunnerIntegrationTest {


    /**
     * To work against AWS uncomment the following service config to CDI create the services
     * Run the test with sysprop: -Dmode=AWS
     */
//    @ConfigProperty(name = "fluidity.services.query")
//    QueryService query;
//
//    @ConfigProperty(name = "fluidity.services.storage")
//    Storage storage;

    QueryService query = new RocksDBQueryService();

    Storage storage = new FileSystemBasedStorageService();

    @Test
    void singleDataflowFileWithSingleCorrelation() {

        /**
         * Uncomment for AWS
         */
//        AwsQueryService awsQueryService = (AwsQueryService) query;
//        awsQueryService.createTable();

        String tenant = "tenant";
        String modelPath = "modelPath-" + System.currentTimeMillis();
        String region = "eu-west-2";
        String session = "TEST-SESSION-ID";
        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(corr)";
        search.to = System.currentTimeMillis();
        search.from = search.to - DateUtil.HOUR;

        final DataflowBuilder dfBuilder = new DataflowBuilder();

        AtomicInteger rewritten = new AtomicInteger();

        populateTestData(region, session, query, storage, tenant);

        WorkflowRunner runnerV1 = new WorkflowRunner(tenant, region, storage, query, dfBuilder, modelPath) {

            @Override
            String rewriteCorrelationData(String tenant, String session, FileMeta[] files, Search search, String modelPath) {
                System.out.println("rewriteCorrelationData!!");
                rewritten.incrementAndGet();
                dfBuilder.extractCorrelationData(session, files, search, storage, region, tenant, modelPath);
                return "done";
            }
        };

        runnerV1.run(search, session);

        // Validate
        assertTrue(rewritten.get() > 0, "Should have rewritten some data");

        ArrayList<String> collected = new ArrayList<>();

        storage.listBucketAndProcess(region, tenant, modelPath, (region1, itemUrl, itemName, modified) -> {
            if (itemName.contains(CORR_HIST_PREFIX)) collected.add(itemUrl);
            return null;
        });

        assertTrue(collected.size() > 0, "Should have found a histogram model in the store");

        // AWS uses bucket name unlike local alternatives
        // String json = new String(storage.get(region, "storgage://fluidity-dev-" + tenant + "/" + collected.get(0), 0));
        String json = new String(storage.get(region, collected.get(0), 0));

        System.out.println("Got Model:" + json.replace("},{", "},\n{"));
        System.out.println("Got Model:" + collected);
        assertTrue(json.contains( "\"opDuration\" : [ 180000, 180000, 180000 ]"), "Missing stats data");
    }

    private void populateTestData(String region, String session, QueryService query, Storage storage, String tenant) {

        String testFilename = "testfile.log";
        StringBuilder testContent = new StringBuilder();
        long startTime = System.currentTimeMillis() - DateUtil.MINUTE * 10;

        testContent.append("\"ts\":\"" + startTime + "\"," + FlowLogHelper.format(session, "builder", "workflow", "Step1")).append("\n");
        testContent.append("\"ts\":\"" + (startTime + DateUtil.MINUTE * 2) + "\"," + FlowLogHelper.format(session, "builder", "workflow", "Step2")).append("\n");
        testContent.append("\"ts\":\"" + (startTime + DateUtil.MINUTE * 3) + "\"," + FlowLogHelper.format(session, "builder", "workflow", "Step3")).append("\n");

        String timeFormat = "prefix:[\"ts\":\"] LONG";
        FileMeta testFile = new FileMeta(tenant, "resource", "tags", testFilename, testContent.toString().getBytes(), System.currentTimeMillis() - DateUtil.MINUTE, System.currentTimeMillis(), timeFormat);
        storage.upload(region, testFile);
        query.put(testFile);
    }
}