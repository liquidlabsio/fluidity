/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

        int[] process = simpleSearchProcessor.process(false, search, 0, System.currentTimeMillis(), 1024, timeFormat);
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


        int[] process = simpleSearchProcessor.process(false, search, 0, System.currentTimeMillis(), 1024, "");
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