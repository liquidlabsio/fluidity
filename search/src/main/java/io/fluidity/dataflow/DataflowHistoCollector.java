package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.histo.HistoAggregatorFun;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoFunction;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;

import java.util.HashMap;
import java.util.Map;

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
    public static final String TOTAL_DURATION = "totalDuration";
    public static final String OP_2_OP_LATENCY = "op2OpLatency";
    public static final String MAX_OP_DURATION = "maxOpDuration";

    // runtime state calculation and state management
    private final Map<String, HistoFunction<Long[], Long>> functionMap = new HashMap<>();

    // final timeseries values for histogram rendering
    private final Map<String, Series<Long[]>> seriesMap = new HashMap<>();

    public DataflowHistoCollector(Search search) {
        this.functionMap.put(TOTAL_DURATION, new HistoAggregatorFun());
        this.seriesMap.put(TOTAL_DURATION, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(OP_2_OP_LATENCY, new HistoAggregatorFun());
        this.seriesMap.put(OP_2_OP_LATENCY, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(MAX_OP_DURATION, new HistoAggregatorFun());
        this.seriesMap.put(MAX_OP_DURATION, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));
    }

    public void add(long currentTime, FlowInfo flowValue) {

        long duration = flowValue.getDuration();
        updateSeries(TOTAL_DURATION, currentTime, duration);

        long[] minMaxOpIntervalWithOpDuration = flowValue.getMinOp2OpLatency();
        updateSeries(OP_2_OP_LATENCY, currentTime, minMaxOpIntervalWithOpDuration[1]);
        updateSeries(MAX_OP_DURATION, currentTime, minMaxOpIntervalWithOpDuration[2]);
    }

    private void updateSeries(String seriesType, long currentTime, long value) {
        Series<Long[]> durationSeries = seriesMap.get(seriesType);
        HistoFunction<Long[], Long> durationFunction = functionMap.get(seriesType);
        durationSeries.update(currentTime, durationFunction.calculate(durationSeries.get(currentTime), value, "nextLine", 0, currentTime, durationSeries.index(currentTime), "expression"));
    }

    public String results() {
        try {
            return new ObjectMapper().writeValueAsString(seriesMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return e.toString();
        }

    }
}