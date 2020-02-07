package io.precognito.services;

import io.precognito.services.search.Search;
import io.precognito.services.search.SearchResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;

import static io.restassured.RestAssured.given;

@QuarkusTest
class SearchResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Inject
    SearchResource search;


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

        String fileUrl = "s3://bucket/fileUrl";
        Search search = new Search();
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given()//.contentType("application/json")
                .multiPart("search", search)
                .when()
                .pathParam("tenant", "tenant")
                .pathParam("files", new String[] { fileUrl, fileUrl })
                .post("/search/files/{tenant}/{files}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }
    @Test
    public void testFinalize() {
        String fileUrl = "fileUrl";
        Search search = new Search();
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given()
                .multiPart("search", search)
                .when()
                .pathParam("files", new String[] { fileUrl })
                .pathParam("tenant", "tenant")
                .post("/search/finalize/{tenant}/{files}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }
}