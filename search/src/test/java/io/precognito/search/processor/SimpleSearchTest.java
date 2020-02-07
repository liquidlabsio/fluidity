package io.precognito.search.processor;

import io.precognito.search.Search;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class SimpleSearchTest {

    @Test
    public void testSubmit() {

//        Search search = new Search();
//        search.expression = "this is a test";
//        ExtractableResponse<Response> response = given().contentType("application/json")
//                .body(search)
//                .when()
//                .post("/search/submit")
//                .then()
//                .statusCode(200).extract();
//        String[] as = response.body().as(String[].class);
//        System.out.println("Got:" + Arrays.toString(as));
    }


}