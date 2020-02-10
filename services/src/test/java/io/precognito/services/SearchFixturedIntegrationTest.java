package io.precognito.services;

import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.search.SearchResource;
import io.precognito.services.storage.StorageResource;
import io.precognito.test.IntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@IntegrationTest
class SearchFixturedIntegrationTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Inject
    StorageResource storageResource;

    @Inject
    SearchResource searchResource;

    @Test
    public void testSearchWorks() throws Exception {

        upload();

        Search search = getSearch();

        System.out.printf("Executing: %s%n", search);
        List<FileMeta> fileUrls = search(search);

        System.out.printf("Searching these files:%s%n", fileUrls);

        List<String> histoEventsPair = searchFile(fileUrls.get(0), search);

        System.out.printf("Search results available here:%s%n", histoEventsPair);

        String[] fileResults = finalizeSearchResults(search, histoEventsPair.get(0), histoEventsPair.get(1));

        System.out.println(fileResults[0]);
    }

    @NotNull
    private Search getSearch() {
        Search search = new Search();
        search.origin = "123";
        search.uid = "UID-"+ System.currentTimeMillis();
        search.expression = "*";
        search.from = 0;
        search.to = System.currentTimeMillis() + 1000;
        return search;
    }

    private String[] finalizeSearchResults(Search search, String histo, String events) {

        String[] results = searchResource.finaliseResults("TENANT", histo, events, search);
        assertTrue(results.length > 0);
        return results;
    }


    private List<String> searchFile(FileMeta file, Search search) {
        return Arrays.asList(searchResource.file("TENANT", new String[] { file.getStorageUrl()}, new Long[] { file.getToTime() }, search));
    }

    private List<FileMeta> search(Search search) {
        FileMeta[] files = searchResource.submit(search);
        assertNotNull(files);
        assertEquals("No files listed", 1, files.length);
        return Arrays.asList(files);
    }

    public void upload() throws Exception {
        String filename = "test-data/file-to-upload.txt";
        final byte[] bytes = IOUtils.toByteArray(new FileInputStream(filename));
        FileMeta fileMeta = new FileMeta("ng-test", "IoTDevice",
                "tag1, tag2", filename, bytes, System.currentTimeMillis()-10000, System.currentTimeMillis());

        storageResource.uploadFile(fileMeta);
    }

}