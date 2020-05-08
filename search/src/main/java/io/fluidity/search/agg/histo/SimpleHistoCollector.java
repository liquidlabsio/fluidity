package io.fluidity.search.agg.histo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Pair;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final EconomicMap<String, Series> seriesMap = EconomicMap.create();

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

        Pair<String, Object> seriesNameAndValue = search.getFieldNameAndValue(sourceName, nextLine);
        if (seriesNameAndValue != null) {
            String groupBy = search.applyGroupBy(tags, sourceName);
            seriesNameAndValue = Pair.create(groupBy + "-" + seriesNameAndValue.getLeft(), seriesNameAndValue.getRight());
            Series series = getSeriesItem(groupBy, seriesNameAndValue.getLeft());
            series.update(currentTime, function.calculate(series.get(currentTime), seriesNameAndValue.getRight(), nextLine, position, currentTime, search.expression));
        }
    }

    private Series getSeriesItem(String groupBy, String seriesName) {
        if (!seriesMap.containsKey(seriesName)){
            seriesMap.put(seriesName, search.getTimeSeries(seriesName, groupBy, from, to));
        }
        return seriesMap.get(seriesName);
    }

    @Override
    public void close() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {

            List<Series> seriesList = StreamSupport.stream(seriesMap.getValues().spliterator(), false).collect(Collectors.toList());
            String histoJson = objectMapper.writeValueAsString(new ArrayList(seriesList));
            outputStream.write(histoJson.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EconomicMap<String, Series> series() {
        return seriesMap;
    }
}
