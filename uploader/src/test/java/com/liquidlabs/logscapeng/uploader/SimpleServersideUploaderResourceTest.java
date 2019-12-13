package com.liquidlabs.logscapeng.uploader;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.MultiPartConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.multipart.MultiPartSpecificationImpl;
import io.restassured.specification.MultiPartSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SimpleServersideUploaderResourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testIdEndpoint() {

        given()
                .when().get("/upload")
                .then()
                .statusCode(200)
                .body(is(SimpleServersideUploaderResource.class.getName()));
    }

    @Test
    public void sendFile() throws Exception {
        String filename = "test-data/file-to-upload2.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        UploadMeta uploadMeta = new UploadMeta("logscape-ng-test-13dec19", "IoTDevice", new String[]{"tag1", "tag2"}, filename, bytes);
        given()
                .multiPart("filecontent", uploadMeta.filename, uploadMeta.filecontent)
                .formParam("filename", uploadMeta.filename)
                .formParam("tenant", uploadMeta.tenant)
                .formParam("resource", uploadMeta.resource)
                .formParam("tags", uploadMeta.tags)
                .when()
                .post("/upload/file")
                .then()
                .statusCode(200)
                .body(is(new String(bytes)+"2"));

    }
    /**
     * http://www.mastertheboss.com/jboss-frameworks/resteasy/using-rest-services-to-manage-download-and-upload-of-files
     *
     */

//    @Test
//    void upload() {
//        SimpleServersideUploaderResource uploader = new SimpleServersideUploaderResource();
//
////        String result = uploader.upload("tenant", "resource", "filename", "meta-json");
//    }
}