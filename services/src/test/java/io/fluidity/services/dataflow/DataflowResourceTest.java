/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        fileMetas[0].setStorageUrl("s3://bucket/somePath/to/file.log");
        Search search = new Search();
        search.origin = "123";
        search.uid = "my-uid";
        search.expression = "*|*|*|field.getJsonPair(corr)";
        search.from = System.currentTimeMillis() - DateUtil.HOUR;
        search.to = System.currentTimeMillis();

        String url = "http://localhost:8081";
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