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

        String[] histoEventsStats = searchFile(fileUrls.get(0), search);

        System.out.printf("Search results available here:%s%n", histoEventsStats[1]);

        String[] eventResults = finalizeEvents(search, histoEventsStats[0], histoEventsStats[1]);

        System.out.println("EventResults:" + Arrays.toString(eventResults));

        assertNotNull(eventResults[0]);
        // contains histo data
        assertTrue(eventResults[1].contains("this is some file 1"), "Contents of events not found");

        String histoResults = finalizeHisto(search, histoEventsStats[0], histoEventsStats[1]);

        System.out.println("Hist:" + histoResults.replace("],[", "],\n["));
        assertNotNull(histoResults);
        assertTrue(histoResults.contains("count"), "didnt perform default count analytic");
    }

    @NotNull
    private Search getSearch() {
        Search search = new Search();
        search.origin = "123";
        search.uid = "UID-"+ System.currentTimeMillis();
        search.expression = "*|*|*|*|*";
        //search.expression = "tags.equals(cc)|*|WorkflowRunner|field.getJsonPair(corr)|analytic.countEach()|time.series()|*";
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

    private String[] searchFile(FileMeta fileMeta, Search search) throws JsonProcessingException {
        FileMeta[] fileMetas = {fileMeta};
        String fileMetaJson = new ObjectMapper().writeValueAsString(fileMetas);

        return searchResource.file(TENANT, URLEncoder.encode(fileMetaJson), search).get(0);
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
                "cc", filename, bytes, System.currentTimeMillis() - DateUtil.HOUR, System.currentTimeMillis(), "");

        storageResource.uploadFile(fileMeta);
    }

}