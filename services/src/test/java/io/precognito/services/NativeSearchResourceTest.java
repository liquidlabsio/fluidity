package io.precognito.services;

import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.util.Arrays;

import static io.restassured.RestAssured.given;

// TODO: revisit -
//  error  Unable to automatically find native image, please set the native.image.path to the native executable you wish to test
//@NativeImageTest
@QuarkusTest
class NativeSearchResourceTest {
    String filename = "test-data/file-to-upload.txt";

    @BeforeEach
    void setUp() {
        try {
            upload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @Inject
    StorageResource storageResource;

    @Test
    public void testSubmit() {

        Search search = new Search();
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given().contentType("application/json")
                .body(search)
                .when()
                .post("/search/submit")
                .then()
                .statusCode(200).extract();
        FileMeta[] as = response.body().as(FileMeta[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }

    public void upload() throws Exception {

        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("tenant", "resource",
                "tag1, tag2", filename, bytes, System.currentTimeMillis() - 10000, System.currentTimeMillis(), "");

        storageResource.uploadFile(fileMeta);
    }

}