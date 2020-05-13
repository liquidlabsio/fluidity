package io.fluidity.services.dataflow;

import io.fluidity.search.Search;
import io.fluidity.util.DateUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DataflowResourceTest {

    @Inject
    DataflowResource dataflowResource;

    @Test
    void id() {
        ExtractableResponse<Response> response = given()
                .when().get("/dataflow/id")
                .then()
                .statusCode(200)
                .extract();
        System.out.println("Got:" + response);

    }

    @Test
    void submit() {
        // test port uses :8081
        // post
        // post
        // submit(String tenant, Search search, String serviceAddress, String modelName)
        Search search = new Search();
        search.origin = "123";
        search.uid = "my-uid";
        search.expression = "*|*|*|field.getJsonPair(corr)";
        search.from = System.currentTimeMillis() - DateUtil.HOUR;
        search.to = System.currentTimeMillis();

        String modelName = "testModel";
        String serviceAddress = "http://localhost:8081/";


        ExtractableResponse<Response> response = given()
                .when()
                .multiPart("origin", search.origin)
                .multiPart("uid", search.uid)
                .multiPart("expression", search.expression)
                .multiPart("from", search.from)
                .multiPart("to", search.to)

                .pathParam("tenant", "tenant")
                .pathParam("modelName", modelName)
                .pathParam("serviceAddress", serviceAddress)// URLEncoder.encode(serviceAddress))
                .when()
                .post("/dataflow/submit/{tenant}/{serviceAddress}/{modelName}")
                .then()
                .statusCode(200).extract();
        String[] as = response.body().as(String[].class);


        System.out.println("Got:" + response);

    }
}