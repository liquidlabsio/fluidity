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

class CountEachHistoAggregatorTest {

    @Test
    void processCount() throws Exception {

        Search search = new Search();
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
        CountEachHistoAggregator aggregator = new CountEachHistoAggregator(inputStreams, search);
        String histogram = aggregator.process();
        assertNotNull(histogram);
        assertTrue(histogram.contains("someFile111"));
        assertTrue(histogram.contains("someFile222"));
    }

    private void generateSeriesData(Search search, Map<String, InputStream> inputStreams, long to, long from, String seriesName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleHistoCollector histoCollector = new SimpleHistoCollector(baos, seriesName, "test-tags", search, from, to, HistoAggFactory.Count);
        histoCollector.add(from, 1000, "someLine");
        histoCollector.close();

        inputStreams.put(seriesName, new ByteArrayInputStream(baos.toByteArray()));
    }
}