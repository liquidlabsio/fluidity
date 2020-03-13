package io.precognito.search.agg.histo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import io.precognito.search.processor.HistoCollector;
import io.precognito.search.processor.HistoFunction;
import io.precognito.search.processor.Series;
import io.precognito.util.DateUtil;

import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *   [
 *     {
 *       name: "Series 1",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     },
 *      {
 *       name: "Series 2",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     }
 *   ]
 */
public class SimpleHistoCollector implements HistoCollector {
    private final long from;
    private final long to;
    private final HistoFunction function;
    private final String sourceName;
    private OutputStream outputStream;
    private final String tags;
    private final String storageUrl;
    private Search search;
    private final Map<String, Series> seriesMap = new HashMap<>();

    public SimpleHistoCollector(OutputStream outputStream, String sourceName, String tags, String storageUrl, Search search, long from, long to, HistoFunction histoFunction) {
        this.outputStream = outputStream;
        this.tags = tags;
        this.storageUrl = storageUrl;
        this.search = search;
        this.sourceName = sourceName;
        this.from = from;
        this.to = to;
        this.function = histoFunction;
    }

    @Override
    public void add(long currentTime, long position, String nextLine) {

        // get the sourceName from the search - default it to sourcename (i.e. _filename) - otherwise extract the
        AbstractMap.SimpleEntry<String, Long> seriesNameandValue = search.getSeriesNameAndValue(sourceName, nextLine);
        if (seriesNameandValue != null) {
            Series series = getSeriesItem(seriesNameandValue.getKey());
            series.update(currentTime, function.calculate(series.get(currentTime), seriesNameandValue.getValue(), nextLine, position, currentTime, search.expression));
        }
    }

    private Series getSeriesItem(String seriesName) {
        seriesMap.computeIfAbsent(seriesName, item -> new Series(seriesName, DateUtil.floorMin(from), DateUtil.floorMin(to)));
        return seriesMap.get(seriesName);
    }

    @Override
    public void close() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String histoJson = objectMapper.writeValueAsString(new ArrayList(seriesMap.values()));
        outputStream.write(histoJson.getBytes());
        outputStream.close();
    }

    public Map<String, Series> series() {
        return seriesMap;
    }
}
