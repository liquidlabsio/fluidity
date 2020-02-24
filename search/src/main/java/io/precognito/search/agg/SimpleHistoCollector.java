package io.precognito.search.agg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import io.precognito.search.processor.HistoCollector;
import io.precognito.search.processor.HistoFunction;
import io.precognito.search.processor.Series;
import io.precognito.util.DateUtil;

import java.io.OutputStream;

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
    private final HistoFunction function;
    Series series = null;
    private OutputStream outputStream;
    private Search search;

    public SimpleHistoCollector(OutputStream outputStream, String filename, String tags, String storageUrl, Search search, long from, long to) {
        this.outputStream = outputStream;
        this.search = search;
        series = new Series(filename, DateUtil.floorMin(from), DateUtil.floorMin(to));
        function = (currentValue, nextLine, position, time, expression) -> currentValue + 1;
    }

    @Override
    public void add(long currentTime, long position, String nextLine) {
        long currentValue = series.get(currentTime);
        // apply some injected function to calculate new value
        series.update(currentTime,function.calculate(currentValue, nextLine, position, currentTime, search.expression));
    }

    @Override
    public void close() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String histoJson = objectMapper.writeValueAsString(series);
        outputStream.write(histoJson.getBytes());
        outputStream.close();
    }
}
