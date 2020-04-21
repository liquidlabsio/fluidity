package io.fluidity.search.agg.histo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;

import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Generic collection of data and points into series and timeseries mapping
 *
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

        AbstractMap.SimpleEntry<String, Object> seriesNameAndValue = search.getSeriesNameAndValue(sourceName, nextLine);
        if (seriesNameAndValue != null) {
            Series series = getSeriesItem(seriesNameAndValue.getKey());
            series.update(currentTime, function.calculate(series.get(currentTime), seriesNameAndValue.getValue(), nextLine, position, currentTime, search.expression));
        }
    }

    private Series getSeriesItem(String seriesName) {
        seriesMap.computeIfAbsent(seriesName, item -> search.getTimeSeries(seriesName, from, to));
        return seriesMap.get(seriesName);
    }

    @Override
    public void close() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String histoJson = objectMapper.writeValueAsString(new ArrayList(seriesMap.values()));
            outputStream.write(histoJson.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Series> series() {
        return seriesMap;
    }
}
