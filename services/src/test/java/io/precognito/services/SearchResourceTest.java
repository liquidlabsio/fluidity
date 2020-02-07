package io.precognito.services;

import io.precognito.search.Search;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("expression", search.expression)

                .pathParam("tenant", "tenant")
                .pathParam("files",  "s3://bucket/fileUrl")
                .when()
                .post("/search/files/{tenant}/{files}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }
    @Test
    public void testFinalize() {
        Search search = new Search();
        search.origin = "123";
        search.expression = "this is a test";
        ExtractableResponse<Response> response = given()
                .multiPart("origin", search.origin)
                .multiPart("expression", search.expression)
                .when()
                .pathParam("files",  "s3://bucket/fileUrl")
                .pathParam("tenant", "tenant")
                .post("/search/finalize/{tenant}/{files}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);
        System.out.println("Got:" + Arrays.toString(as));
    }
}