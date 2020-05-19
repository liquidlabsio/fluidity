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

package io.fluidity.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.StorageResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                .pathParam("tenant", "tenant")
                .body(search)
                .when()
                .post("/search/submit/{tenant}")
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
        fileMeta.storageUrl = "storage://fixtured-storage-bucket/resource/test-data/file-to-upload.txt";
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
        List<Integer[]> as = response.body().as(ArrayList.class);
        Assert.assertNotNull(as);
        Assert.assertTrue(as.size() > 0);
        System.out.println("Got:" + as.toString());
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