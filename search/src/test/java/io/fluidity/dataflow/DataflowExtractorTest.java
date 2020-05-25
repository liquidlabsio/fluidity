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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataflowExtractorTest {

    @Test
    void processWithCorrelationEnhancements() throws IOException {
        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.toString().getBytes());
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

        rewriter.process(false, search, 0, System.currentTimeMillis(), 1024, "");
        assertEquals(2, collected.size());
        List<Map.Entry<String, String>> datFile = collected.entrySet().stream().filter(entry -> entry.getKey().endsWith(".dat")).collect(Collectors.toList());
        String jsonDatData = datFile.iterator().next().getValue();
        assertEquals(5, jsonDatData.split(":").length, "JSON split by : didnt provide 5 segments json was:" + jsonDatData);
    }

    private StringBuilder makeFileContent() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");
        StringBuilder fileContentAsString = new StringBuilder();
        String data = "\"timestamp\":1587210348,\"txn\":\"99kerob\",\"service\":\"rest\", \"operation\":\"myOp\"" +
                "\"type\":\"microservice\", \"meta\":\"errors\"";

        long time = System.currentTimeMillis() - DateUtil.MINUTE;

        for (int i = 10; i < 100; i++) {
            fileContentAsString.append(dateTimeFormatter.print(time)).append(" ");
            fileContentAsString.append(data);
            fileContentAsString.append('\n');
            time += 1000;
        }
        return fileContentAsString;
    }

}