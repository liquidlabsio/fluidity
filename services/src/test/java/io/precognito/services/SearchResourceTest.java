package io.precognito.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.net.URLEncoder;
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
        FileMeta[] as = response.body().as(FileMeta[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }


    @Test
    public void testFileSearch() throws com.fasterxml.jackson.core.JsonProcessingException {

        /**
         * Note: file url arrays dont get passed properly from RestAssured
         */
        Search search = new Search();
        search.origin = "123";
        search.uid = "my-uid";
        search.expression = "this is a test";

        FileMeta fileMeta = new FileMeta();
        fileMeta.storageUrl = "s3://fixtured-storage-bucket/resource/test-data/file-to-upload.txt";
        fileMeta.toTime = System.currentTimeMillis();
        fileMeta.filename = "yay";


        FileMeta[] fileMetas = {fileMeta};
        String fileMetaJson = new ObjectMapper().writeValueAsString(fileMetas);

        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("uid", search.uid)
                .multiPart("expression", search.expression)

                .pathParam("tenant", "tenant")
                .pathParam("files", URLEncoder.encode(fileMetaJson))
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
    public void testFinalizeEvents() throws Exception {
        // need the data to be searched for finalize to work
        testFileSearch();
        Search search = new Search();
        search.origin = "123";
        search.expression = "this is a test";


        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("expression", search.expression)
                .when()
                .pathParam("tenant", "tenant")
                .pathParam("fromTime", "0")
                .post("/search/finalizeEvents/{tenant}/{fromTime}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        Assert.assertNotNull(as);
        Assert.assertTrue(as.length > 0);
        System.out.println("Got:" + Arrays.toString(as));
    }

    public void upload() throws Exception {
        FileMeta fileMeta = new FileMeta("tenant", "resource",
                "tag1, tag2", filename, "SomeData".getBytes(), System.currentTimeMillis() - 10000, System.currentTimeMillis(), "");

        storageResource.uploadFile(fileMeta);
    }
}