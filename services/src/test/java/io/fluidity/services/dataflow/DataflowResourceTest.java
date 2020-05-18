/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
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
    void rewriteRestClientWorks() {
        FileMeta[] fileMetas = new FileMeta[]{new FileMeta("tenant", "file", "tags", "someFile", "someContent".getBytes(), 100l, 200l, "")};
        fileMetas[0].setStorageUrl("storage://bucket/somePath/to/file.log");
        Search search = new Search();
        search.origin = "123";
        search.uid = "my-uid";
        search.expression = "*|*|*|field.getJsonPair(corr)";
        search.from = System.currentTimeMillis() - DateUtil.HOUR;
        search.to = System.currentTimeMillis();

        String url = "http://localhost:8080";
        try {
            DataflowResource.rewriteCorrelationDataS("someTenant", "session", fileMetas, search, url, "model");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

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
        String as = response.body().as(String.class);

        System.out.println("Got:" + as);

    }
}