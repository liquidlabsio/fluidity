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

package io.fluidity.dataflow;

import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataflowExtractorTest {
    List<Long> times = new ArrayList<>();

    @Test
    void processesMultipleCorrelationIds() throws IOException {
        String TIME_FORMAT = "prefix:[timestamp\":] LONG";
        String corr1 = makeFileContent("111", 1);
        String corr2 = makeFileContent("222", 0);
        ByteArrayInputStream instream = new ByteArrayInputStream((corr1 + corr2).getBytes());
        StorageInputStream inputStream1 = new StorageInputStream("someFile", System.currentTimeMillis(), instream.available(), instream);
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                System.out.println(filePath);
                IOUtils.copy(inputStream, baos);
                collected.put(filePath, baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        DataflowExtractor rewriter = new DataflowExtractor(inputStream1, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0l;
        search.to = System.currentTimeMillis();

        // ***************  NOTE: Guessing the time format = the format is not passed in
        rewriter.process(false, search, System.currentTimeMillis() - DateUtil.MINUTE, System.currentTimeMillis(), 1024, TIME_FORMAT);
        assertEquals(4, collected.size());

        assertTrue(collected.keySet().toString().contains("111-kerob"), "should contain both 111 and 222 correlation keys");
        assertTrue(collected.keySet().toString().contains("222-kerob"), "should contain both 111 and 222 correlation keys");
    }

    @Test
    void processGuessingTime() throws IOException {
        String fileContentAsString = makeFileContent("", 1);
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.getBytes());
        StorageInputStream inputStream1 = new StorageInputStream("someFile", System.currentTimeMillis(), instream.available(), instream);
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(inputStream, baos);
                collected.put(filePath, baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        DataflowExtractor rewriter = new DataflowExtractor(inputStream1, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0l;
        search.to = System.currentTimeMillis();

        // ***************  NOTE: Guessing the time format = the format is not passed in
        rewriter.process(false, search, System.currentTimeMillis()-DateUtil.MINUTE, System.currentTimeMillis(), 1024, "");
        assertEquals(2, collected.size());
        assertFilenameTimesAreGuessedCorrect(collected.keySet().iterator().next());
    }

    /**
     *  Filename is of the form: filePrefix/dat_99kerob_422652135743_1593073434728_.dat
     */
    private void assertFilenameTimesAreGuessedCorrect(String filename) {
        String[] filenamePaths = filename.split("_");
        long fromTime = Long.parseLong(filenamePaths[1]);
        long toTime = Long.parseLong(filenamePaths[2]);

        long delta = toTime - fromTime;
        System.out.println("Times:" + filename + " Delta:" + delta);
        System.out.println("Start:" + new DateTime(fromTime));
        System.out.println("End" + new DateTime(toTime));

        assertTrue(fromTime < toTime, " File times are not correct - got:" + filename);
        // should guess less than 60 seconds
        assertTrue( delta < 60 * 1000, " Delta is way too large:" + delta);
    }

    @Test
    void processWithCorrelationEnhancementsAndTimeFormat() throws IOException {
        String fileContentAsString = makeFileContent("", 0);
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.getBytes());
        StorageInputStream inputStream1 = new StorageInputStream("someFile", System.currentTimeMillis(), instream.available(), instream);
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(inputStream, baos);
                collected.put(filePath, baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        DataflowExtractor rewriter = new DataflowExtractor(inputStream1, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0l;
        search.to = System.currentTimeMillis();

        String TIME_FORMAT = "prefix:[timestamp\":] LONG";

        rewriter.process(false, search, 0, System.currentTimeMillis(), 1024, TIME_FORMAT);
        assertEquals(2, collected.size());
        List<Map.Entry<String, String>> datFile = collected.entrySet().stream().filter(entry -> entry.getKey().endsWith(".dat")).collect(Collectors.toList());
        String jsonDatData = datFile.iterator().next().getValue();

        assertFilenameTimesAreCorrect(collected.keySet().iterator().next());
        assertEquals(6, jsonDatData.split(":").length, "JSON split by : didnt provide 5 segments json was:" + jsonDatData);
    }

    /**
     *  Filename is of the form: filePrefix/dat_422652135743_1593073434728_99kerob_.dat
     */
    private void assertFilenameTimesAreCorrect(String filename) {
        String[] filenamePaths = filename.split("_");
        long fromTime = Long.parseLong(filenamePaths[1]);
        long toTime = Long.parseLong(filenamePaths[2]);

        System.out.println("Times:" + times.toString());

        assertTrue(fromTime < toTime, " File times are not correct - got:" + filename);

        assertEquals(fromTime, times.get(0));
        assertEquals(toTime, times.get(times.size()-1), "Didnt get the LAST time correctlys");
    }

    private String makeFileContent(String prefix, int mins_ago) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.ss");
        StringBuilder fileContentAsString = new StringBuilder();
        String corrId = prefix + "-kerob";
        String data = "\"timestamp\":_TS_,\"txn\":\"" + corrId + "\",\"service\":\"rest\", \"operation\":\"myOp\"" +
                "\"type\":\"microservice\", \"meta\":\"errors\"  TTTT:";

        long time = DateUtil.floorMin(System.currentTimeMillis() - DateUtil.MINUTE);
        time -= mins_ago * DateUtil.MINUTE;

        for (int i = 10; i < 20; i++) {
            fileContentAsString.append(data.replace("_TS_", Long.toString(time)));
            fileContentAsString.append(dateTimeFormatter.print(time)).append(" ");
            fileContentAsString.append('\n');
            times.add(time);
            time += 1000;
        }

        return fileContentAsString.toString();
    }

}