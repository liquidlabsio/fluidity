package io.precognito.search.agg;

import io.precognito.search.Search;
import io.precognito.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeriesHistoAggregatorTest {

    @Test
    void process() throws Exception {

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
        SeriesHistoAggregator aggregator = new SeriesHistoAggregator(inputStreams, search);
        String histogram = aggregator.process();
        assertNotNull(histogram);
        assertTrue(histogram.contains("someFile111"));
        assertTrue(histogram.contains("someFile222"));

        System.out.printf("Got: \n%s", histogram);
    }

    private void generateSeriesData(Search search, Map<String, InputStream> inputStreams, long to, long from, String seriesName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleHistoCollector histoCollector = new SimpleHistoCollector(baos, seriesName, "tags", "s3://blah", search, from, to);
        histoCollector.add(from, 1000, "someLine");
        histoCollector.close();

        inputStreams.put(seriesName, new ByteArrayInputStream(baos.toByteArray()));
    }
}