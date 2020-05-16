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

package io.fluidity.search.agg.events;

import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.NoopHistoCollector;
import io.fluidity.util.DateUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchEventCollectorTest {

    @Test
    public void testSearchGetsTime() throws Exception {

        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        SearchEventCollector simpleSearchProcessor = new SearchEventCollector(new NoopHistoCollector(),
                new ByteArrayInputStream(fileContentAsString.toString().getBytes()), baos);
        Search search = new Search();
        search.expression = "* | * | * | * | CPU | *";
        search.from = 0l;
        search.to = System.currentTimeMillis();

        String timeFormat = "yyyy-MM-dd HH:mm.SS";

        Integer[] process = simpleSearchProcessor.process(false, search, 0, System.currentTimeMillis(), 1024, timeFormat);
        assertTrue(process[0] > 0, "didn't process any data");
        System.out.println("Processed:" + process);
        String outFileContents = new String(baos.toByteArray());
        System.out.println(outFileContents);
    }

    @Test
    public void testSearchGrep() throws Exception {

        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();


        SearchEventCollector simpleSearchProcessor = new SearchEventCollector(new NoopHistoCollector(),
                new ByteArrayInputStream(fileContentAsString.toString().getBytes()), baos);
        Search search = new Search();
        search.expression = "* | * | * | * | CPU | *";
        search.from = 0l;
        search.to = System.currentTimeMillis();


        Integer[] process = simpleSearchProcessor.process(false, search, 0, System.currentTimeMillis(), 1024, "");
        assertTrue(process[0] > 0, "didnt process any data");
        System.out.println("Processed:" + process);
        String outFileContents = new String(baos.toByteArray());
        System.out.println(outFileContents);
    }

    private StringBuilder makeFileContent() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");
        StringBuilder fileContentAsString = new StringBuilder();

        long time = System.currentTimeMillis() - DateUtil.MINUTE;

        for (int i = 10; i < 100; i++) {
            fileContentAsString.append(String.format("%s %s CPU:%d", dateTimeFormatter.print(time), i % 2 == 0 ? "ERROR" : "INFO", i));
            fileContentAsString.append('\n');
            time += 1000;
        }
        return fileContentAsString;
    }

}