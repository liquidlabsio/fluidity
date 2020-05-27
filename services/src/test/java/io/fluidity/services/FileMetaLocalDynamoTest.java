package io.fluidity.services;

import io.fluidity.services.aws.AwsQueryService;
import io.fluidity.services.aws.LocalDynamoDbContainer;
import io.fluidity.services.query.FileMeta;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * For TestContainers to work check docker auth is disabled - in ~/.docker/config.json / remove line:   "credsStore" : "osxkeychain"
 */
@QuarkusTest
public class FileMetaLocalDynamoTest {

    AwsQueryService metaDataService;
    
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
        metaDataService = new AwsQueryService();
        metaDataService.setClient(dynamoTestClient);
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    public void ormTest() throws IOException {
        String tenant = "fluidity-ng-test";
        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta(tenant, "IoTDevice",
                "tag1, tag2", filename, bytes
                , System.currentTimeMillis() - 1000, System.currentTimeMillis(), "");

        fileMeta.setStorageUrl("storage://somewhere");

        metaDataService.bind();
        metaDataService.createTable();
        metaDataService.put(fileMeta);

        byte[] content = metaDataService.get(fileMeta.getTenant(), fileMeta.getFilename(), 0);
        assertNotNull(content);

        metaDataService.putList(Arrays.asList(fileMeta));

        List<FileMeta> listed = metaDataService.list(tenant);
        assertEquals(1, listed.size());

        List<FileMeta> queried = metaDataService.query(fileMeta.getTenant(), fileMeta.getFilename(), "");

        assertEquals(1, queried.size());
        System.out.println("Query:" + queried);

    }
}
