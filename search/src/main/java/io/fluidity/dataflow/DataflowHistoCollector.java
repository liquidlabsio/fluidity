package io.fluidity.dataflow;

import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoFunction;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import org.graalvm.collections.EconomicMap;

/**
 * Generic collection of data and points into series and timeseries mapping
 * <p>
 * [
 * {
 * name: "Series 1",
 * data: [
 * [1486684800000, 34],
 * [1486771200000, 43],
 * [1486857600000, 31] ,
 * [1486944000000, 43],
 * [1487030400000, 33],
 * [1487116800000, 52]
 * ]
 * },
 * {
 * name: "Series 2",
 * data: [
 * [1486684800000, 34],
 * [1486771200000, 43],
 * [1486857600000, 31] ,
 * [1486944000000, 43],
 * [1487030400000, 33],
 * [1487116800000, 52]
 * ]
 * }
 * ]
 */
public class DataflowHistoCollector {
    private final HistoFunction function;
    private final TimeSeries series;
    private final EconomicMap<String, Series> seriesMap = EconomicMap.create();

    public DataflowHistoCollector(Search search, HistoFunction histoFunction) {
        this.function = histoFunction;
        this.series = new TimeSeries("none", "", search.from, search.to);
    }

    public void add(long currentTime, FlowInfo value) {
        series.update(currentTime, function.calculate(series.get(currentTime), value, "nextLine", 0, currentTime, series.index(currentTime), "expression"));
    }

    public EconomicMap<String, Series> series() {
        return seriesMap;
    }
}
