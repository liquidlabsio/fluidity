package com.logscapeng.uploader;

import com.logscapeng.uploader.aws.AwsFileMetaDataService;
import com.logscapeng.uploader.aws.LocalDynamoDbContainer;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
        // Note: CIRCLE CI AWS credential for when error msg below received
        // 13:54:22 WARN  [software.amazon.awssdk.regions.internal.util.EC2MetadataUtils] Unable to retrieve the requested metadata.
        //software.amazon.awssdk.core.exception.SdkClientException: Unable to load region from any of the providers in the chain
        // software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain@3ef0e576:
        // [software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider@79604abe:
        // Unable to load region from system settings. Region must be specified either via environment variable (AWS_REGION)
        // or  system property (aws.region).,
        // software.amazon.awssdk.regions.providers.AwsProfileRegionProvider@5834198f: No region provided in profile: default,
        // software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider@5d84e363: Unable to retrieve region information from EC2 Metadata service.
        // Please make sure the application is running on EC2.]
        System.setProperty("aws.region", "eu-west-2");
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
    AwsFileMetaDataService metaDataService;

    @Test
    public void ormTest() throws InterruptedException, IOException {
        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("logscape-ng-test", "IoTDevice",
                "tag1, tag2", filename, bytes
                , System.currentTimeMillis()-1000, System.currentTimeMillis());

        metaDataService.createTable();
        metaDataService.put(fileMeta);
        FileMeta fileMeta1 = metaDataService.get(fileMeta.getTenant(), fileMeta.getFilename());
        assertNotNull(fileMeta1);

        List<FileMeta> listed = metaDataService.list();
        assertEquals(1, listed.size());

        List<FileMeta> queried = metaDataService.query(fileMeta.getTenant(), fileMeta.getFilename(), "");

        assertEquals(1, queried.size());
        System.out.println("Query:" + queried);

    }
}
