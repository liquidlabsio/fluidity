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

package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;
import io.fluidity.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountHistoAggregatorTest {

    @Test
    void processCountGroupBy() throws Exception {

        Search search = new Search();
        search.expression = "*| *| *| *| *| *| groupBy(tag)";
        Map<String, InputStream> inputStreams = new HashMap<>();

        /**
         * Map/Build a non-aggregated series for a single source
         */
        long to = System.currentTimeMillis();
        long from = to - 5 * DateUtil.MINUTE;

        generateSeriesData(search, inputStreams, to, from, "someFile111");
        generateSeriesData(search, inputStreams, to, from, "someFile222");

        /**
         * Reduce/Merge them together
         */
        CountHistoAggregator aggregator = new CountHistoAggregator(inputStreams, search);
        String histogram = aggregator.process();
        assertNotNull(histogram);
        assertTrue(histogram.contains("tag-value"));
    }

    @Test
    void process() throws Exception {

        Search search = new Search();
        search.expression = "*|*|*|*|*|*";
        Map<String, InputStream> inputStreams = new HashMap<>();

        /**
         * Map/Build a non-aggregated series for a single source
         */
        long to = System.currentTimeMillis();
        long from = to - 5 * DateUtil.MINUTE;

        generateSeriesData(search, inputStreams, to, from, "someFile111");
        generateSeriesData(search, inputStreams, to, from, "someFile222");

        search.from = from;
        search.to = to;


        /**
         * Reduce/Merge them together
         */
        CountHistoAggregator aggregator = new CountHistoAggregator(inputStreams, search);
        String histogram = aggregator.process();
        assertNotNull(histogram);
        assertTrue(histogram.contains("count"));

        System.out.printf("Got: \n%s", histogram);
    }

    private void generateSeriesData(Search search, Map<String, InputStream> inputStreams, long to, long from, String seriesName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleHistoCollector histoCollector = new SimpleHistoCollector(baos, search, from, to, HistoAggFactory.Count);
        histoCollector.updateFileInfo(seriesName, "tag-value");
        histoCollector.add(from, 1000, "someLine");
        histoCollector.close();

        inputStreams.put(seriesName, new ByteArrayInputStream(baos.toByteArray()));
    }
}