package com.logscapeng.uploader;

import com.logscapeng.uploader.aws.AwsFileMetaDataQueryService;
import com.logscapeng.uploader.aws.LocalDynamoDbContainer;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

//import org.testcontainers.dynamodb.DynaliteContainer;

/**
 * For TestContainers to work check docker auth is disabled - in ~/.docker/config.json / remove line:   "credsStore" : "osxkeychain"
 *
 */
@QuarkusTest
public class DynamoIndexingTest {

    private static LocalDynamoDbContainer dynamoDB;
    private static DynamoDbClient dynamoTestClient;

    @BeforeAll
    static void startContainer() {
        // create it here to prevent non-related classes from creating the static instance
        dynamoDB = new LocalDynamoDbContainer();
        dynamoDB.start();
        dynamoTestClient = dynamoDB.getAws2Client(dynamoDB.getContainerIpAddress(), dynamoDB.getFirstMappedPort());
    }
    @AfterAll
    static void stopContainer() {
        dynamoDB.stop();
    }

    @BeforeEach
    void setUp() {
        metaDataService.setClient(dynamoTestClient);
    }

    @AfterEach
    void tearDown() {
    }

    @Inject
    AwsFileMetaDataQueryService metaDataService;

    @Test
    public void ormTest() throws InterruptedException, IOException {
        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("logscape-ng-test", "IoTDevice",
                "tag1, tag2", filename, bytes
                , System.currentTimeMillis()-1000, System.currentTimeMillis());

        metaDataService.createTable();
        metaDataService.put(fileMeta);
        byte[] content  = metaDataService.get(fileMeta.getTenant(), fileMeta.getFilename());
        assertNotNull(content);

        List<FileMeta> listed = metaDataService.list();
        assertEquals(1, listed.size());

        List<FileMeta> queried = metaDataService.query(fileMeta.getTenant(), fileMeta.getFilename(), "");

        assertEquals(1, queried.size());
        System.out.println("Query:" + queried);

    }
}
