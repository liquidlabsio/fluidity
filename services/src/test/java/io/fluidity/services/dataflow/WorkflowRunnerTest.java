package io.fluidity.services.dataflow;

import io.fluidity.dataflow.LogHelper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.server.FileSystemBasedStorageService;
import io.fluidity.services.server.RocksDBQueryService;
import io.fluidity.services.storage.Storage;
import io.fluidity.util.DateUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.CORR_HIST_PREFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WorkflowRunnerTest {


    /**
     * To work against AWS uncomment the following service config to CDI create the services
     * Run the test with sysprop: -Dmode=AWS
     */
//    @Inject
//    DynamoDbClient dynamoDB;
//
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
        String modelPath = "modelPath";
        String region = "eu-west-2";
        String session = "TEST-SESSION-ID";
        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(corr)";
        search.from = System.currentTimeMillis() - DateUtil.HOUR;
        search.to = System.currentTimeMillis();

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

        storage.listBucketAndProcess(region, tenant, modelPath + CORR_HIST_PREFIX, (region1, itemUrl, itemName) -> {
            collected.add(itemUrl);
            return null;
        });

        assertTrue(collected.size() > 0, "Should have found a histogram model in the store");

        // AWS uses bucket name unlike local alternatives
        // String json = new String(storage.get(region, "s3://fluidity-dev-" + tenant + "/" + collected.get(0), 0));
        String json = new String(storage.get(region, collected.get(0), 0));

        System.out.println("Got Model:" + json);
        assertTrue(json.contains("totalDuration"), "Missing duration series");
        assertTrue(json.contains("op2OpLatency"), "Missing op2OpLatency series");
        assertTrue(json.contains("maxOpDuration"), "Missing maxOpDuration series");
        assertTrue(json.contains("\\\"right\\\":[10260,10260,10260,1]"), "Missing maxOpDuration data");
    }

    private void populateTestData(String region, String session, QueryService query, Storage storage, String tenant) {
        String testFilename = "testfile.log";
        StringBuilder testContent = new StringBuilder();
        testContent.append(LogHelper.format(session, "builder", "workflow", "Step1")).append("\n");
        testContent.append(LogHelper.format(session, "builder", "workflow", "Step2")).append("\n");
        testContent.append(LogHelper.format(session, "builder", "workflow", "Step3")).append("\n");

        FileMeta testFile = new FileMeta(tenant, "resource", "tags", testFilename, testContent.toString().getBytes(), System.currentTimeMillis() - DateUtil.MINUTE, System.currentTimeMillis(), "");
        storage.upload(region, testFile);
        query.put(testFile);
    }
}