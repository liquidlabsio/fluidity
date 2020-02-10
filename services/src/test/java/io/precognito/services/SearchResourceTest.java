package io.precognito.services;

import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static io.restassured.RestAssured.given;

@QuarkusTest
class SearchResourceTest {
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
        String[] as = response.body().as(String[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }


    @Test
    public void testFileSearch() {

        /**
         * Note: file url arrays dont get passed properly from RestAssured
         */
        Search search = new Search();
        search.origin = "123";
        search.uid = "my-uid";
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("uid", search.uid)
                .multiPart("expression", search.expression)

                .pathParam("tenant", "tenant")
                .pathParam("files",  "s3://fixtured-storage-bucket/resource/test-data/file-to-upload.txt")
                .when()
                .post("/search/files/{tenant}/{files}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        Assert.assertNotNull(as);
        Assert.assertTrue(as.length > 0);
        System.out.println("Got:" + Arrays.toString(as));
    }
    @Test
    public void testFinalize() throws JsonProcessingException, UnsupportedEncodingException {
        // need the data to be searched for finalize to work
        testFileSearch();
        Search search = new Search();
        search.origin = "123";
        search.expression = "this is a test";


        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("expression", search.expression)
                .when()
                .pathParam("histos", "[]")
                .pathParam("events",  "s3://tenant/search-staging/my-uid/resource/test-data/file-to-upload.txt")
                .pathParam("tenant", "tenant")
                .post("/search/finalize/{tenant}/{histos}/{events}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        Assert.assertNotNull(as);
        Assert.assertTrue(as.length > 0);
        System.out.println("Got:" + Arrays.toString(as));
    }

    public void upload() throws Exception {

        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("tenant", "resource",
                "tag1, tag2", filename, bytes, System.currentTimeMillis()-10000, System.currentTimeMillis());

        storageResource.uploadFile(fileMeta);
    }
}