package io.precognito.services;

import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageResource;
import io.precognito.util.UriUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/**
 * TODO: make work for native build checking
 */
//@NativeImageTest
@QuarkusTest
class NativeStorageResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testIdEndpoint() throws URISyntaxException, UnsupportedEncodingException {


        String badURL = "s3://precognito-prod-tenant-userstore/exportedlogs/e37ac5c7-0170-416c-939c-19007ec3e1af/2020-02-27-[$LATEST]0067289175a6449aa06c17778d756505/000000.gz";

        String[] hostnameAndPath = UriUtil.getHostnameAndPath(badURL);

        System.out.println(hostnameAndPath);

        String goodUrl = badURL.replace("(", "%28").replace(")", "'%29");
        String goodUrl2 = goodUrl.replace("[", "%5B").replace("]", "'%5D");
        String goodUrl3 = goodUrl.replace("$", "%24");

        System.out.println("GOT:" + goodUrl3);

        URI uri = new URI(goodUrl2);

        System.out.println(goodUrl2);

        given()
                .when().get("/storage")
                .then()
                .statusCode(200)
                .body(is(StorageResource.class.getName()));
    }

    @Test
    public void sendFile() throws Exception {

        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("precog-ng-test", "IoTDevice",
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

    }
}