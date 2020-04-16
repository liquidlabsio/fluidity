package io.fluidity.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.search.SearchResource;
import io.fluidity.services.storage.StorageResource;
import io.fluidity.test.IntegrationTest;
import io.fluidity.util.DateUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@IntegrationTest
class SearchFixturedIntegrationTest {

    public static final String TENANT = "TENANT";

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

        String[] eventResults = finalizeEvents(search, histoEventsPair.get(0), histoEventsPair.get(1));

        assertNotNull(eventResults[0]);
        // contains histo data
        assertTrue(eventResults[1].contains("this is some file 1"), "Contents of events not found");

        String histoResults = finalizeHisto(search, histoEventsPair.get(0), histoEventsPair.get(1));

        assertNotNull(histoResults);
        assertTrue(histoResults.contains("count"), "didnt perform default count analytic");
    }

    @NotNull
    private Search getSearch() {
        Search search = new Search();
        search.origin = "123";
        search.uid = "UID-"+ System.currentTimeMillis();
        search.expression = "*|*|*|*|*";
        search.from =  System.currentTimeMillis() - DateUtil.DAY;
        search.to = System.currentTimeMillis() + 1000;
        return search;
    }

    private String[] finalizeEvents(Search search, String histo, String events) {

        String[] results = searchResource.finaliseEvents(TENANT, search, 0);
        assertTrue(results.length > 0);
        return results;
    }

    private String finalizeHisto(Search search, String histo, String events) {
        return searchResource.finaliseHisto(TENANT, search);
    }

    private List<String> searchFile(FileMeta fileMeta, Search search) throws JsonProcessingException {
        FileMeta[] fileMetas = {fileMeta};
        String fileMetaJson = new ObjectMapper().writeValueAsString(fileMetas);

        return Arrays.asList(searchResource.file(TENANT, URLEncoder.encode(fileMetaJson), search));
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
                "tag1, tag2", filename, bytes, System.currentTimeMillis() - 10000, System.currentTimeMillis(), "");

        storageResource.uploadFile(fileMeta);
    }

}