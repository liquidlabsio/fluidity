package io.fluidity.services;

import io.fluidity.services.query.FileMeta;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@QuarkusTest
class QueryResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testUploadListQuery() throws Exception {
        FileMeta fileMeta = sendFile();
        executeList();
        executeQuery(fileMeta);
    }

    private void executeQuery(FileMeta fileMeta) {
        ExtractableResponse<Response> response = given()
                .queryParam("tenant", fileMeta.tenant)
                .queryParam("filenamePart", fileMeta.filename)
                .queryParam("tagNamePart", fileMeta.tags)
                .when().get("/query/query")
                .then()
                .statusCode(200)
                .extract();

        List<FileMeta> fileMetaList = Arrays.asList(response.body().as(FileMeta[].class));

        assertNotNull(fileMetaList);
        assertEquals("Query.list() was empty", 1, fileMetaList.size());
        System.out.println("List:" + fileMetaList);
    }

    private void executeList() {
        ExtractableResponse<Response> response = given()
                .when().get("/query/list")
                .then()
                .statusCode(200)
                .extract();

        List<FileMeta> fileMetaList = Arrays.asList(response.body().as(FileMeta[].class));

        assertNotNull(fileMetaList);
        assertEquals("Query.list() was empty", 1, fileMetaList.size());
    }

    public FileMeta sendFile() throws Exception {

        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("fluidity-ng-test", "IoTDevice",
                "tag1, tag2", filename, bytes, System.currentTimeMillis() - 1000, System.currentTimeMillis(), "");
        given()
                .multiPart("fileContent", fileMeta.filename, fileMeta.fileContent)
                .formParam("filename", fileMeta.filename)
                .formParam("tenant", fileMeta.tenant)
                .formParam("resource", fileMeta.resource)
                .formParam("tags", fileMeta.tags)
                .when()
                .post("/storage/upload")
                .then()
                .statusCode(200)
                .body(containsString("Uploaded"));

        return fileMeta;
    }
}