package io.precognito.search.agg.histo;

import io.precognito.search.Search;
import io.precognito.search.processor.HistoFunction;

import java.io.InputStream;
import java.util.Map;

/**
 * Returns timeseries histogram data that looks like this:
 *  series: [
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
public interface HistoAggregator extends AutoCloseable {
    String process() throws Exception;

    boolean isForMe(String analytic);

    HistoAggregator clone(Map<String, InputStream> inputStreams, Search search);

    HistoFunction function();
}
