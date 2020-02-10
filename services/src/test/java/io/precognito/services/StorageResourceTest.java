package io.precognito.services;

import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StorageResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testIdEndpoint() {

        given()
                .when().get("/storage")
                .then()
                .statusCode(200)
                .body(is(StorageResource.class.getName()));
    }

    @Test
    public void sendFile() throws Exception {

        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("precog-ng-test", "IoTDevice",
                "tag1, tag2", filename, bytes, System.currentTimeMillis()-1000, System.currentTimeMillis());
        given()
                .multiPart("fileContent", fileMeta.filename, fileMeta.fileContent)
                .formParam("filename", fileMeta.filename)
                .formParam("tenant", fileMeta.tenant)
                .formParam("resource", fileMeta.resource)
                .formParam("tags", fileMeta.tags)
                .when()
                .post("/storage/upload")
                .then()
                .statusCode(200)
                .body(containsString("Uploaded"));

    }
}