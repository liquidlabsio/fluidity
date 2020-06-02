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

package io.fluidity.dataflow;

import io.fluidity.dataflow.histo.DurationStatsFun;
import io.fluidity.dataflow.histo.LatencyLadderFun;
import io.fluidity.dataflow.histo.StatsDuration;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoFunction;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects aggregate statistics across all dataflows
 */
public class DataflowHistoCollector {
    public static final String TOTAL_DURATION = "totalDuration";
    public static final String OP_2_OP_LATENCY = "op2OpLatency";
    public static final String MAX_OP_DURATION = "maxOpDuration";
    public static final String MIN_OP_DURATION = "minOpDuration";
    public static final String FLOW_COUNT = "flowCount";
    public static final String LADDER = "ladder";

    // runtime state calculation and state management
    private final Map<String, HistoFunction<Long[], Long>> functionMap = new HashMap<>();

    // final time series values for histogram rendering
    private final Map<String, Series<Long[]>> seriesMap = new HashMap<>();

    private final LatencyLadderFun ladderFun = new LatencyLadderFun();
    Series<Map<Long, StatsDuration>> ladderSeries;


    public DataflowHistoCollector(Search search) {
        this.functionMap.put(TOTAL_DURATION, new DurationStatsFun());
        this.seriesMap.put(TOTAL_DURATION, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(FLOW_COUNT, new DurationStatsFun());
        this.seriesMap.put(FLOW_COUNT, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(OP_2_OP_LATENCY, new DurationStatsFun());
        this.seriesMap.put(OP_2_OP_LATENCY, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(MAX_OP_DURATION, new DurationStatsFun());
        this.seriesMap.put(MAX_OP_DURATION, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.functionMap.put(MIN_OP_DURATION, new DurationStatsFun());
        this.seriesMap.put(MIN_OP_DURATION, new TimeSeries("none", "", search.from, search.to, new Series.LongOps()));

        this.ladderSeries = new TimeSeries("none", "", search.from, search.to, new Series.LongOps());
    }

    public void add(long currentTime, FlowInfo flowValue) {

        updateSeries(FLOW_COUNT, currentTime, 1);

        long duration = flowValue.getDuration();
        updateSeries(TOTAL_DURATION, currentTime, duration);


        long[] minMaxOpIntervalWithOpDuration = flowValue.getMinOp2OpLatency();
        updateSeries(MIN_OP_DURATION, currentTime, minMaxOpIntervalWithOpDuration[0]);
        updateSeries(MAX_OP_DURATION, currentTime, minMaxOpIntervalWithOpDuration[1]);
        updateSeries(OP_2_OP_LATENCY, currentTime, minMaxOpIntervalWithOpDuration[2]);

        updateLadder(currentTime, minMaxOpIntervalWithOpDuration[2]);
    }

    private void updateSeries(String seriesType, long currentTime, long value) {
        Series<Long[]> durationSeries = seriesMap.get(seriesType);
        HistoFunction<Long[], Long> durationFunction = functionMap.get(seriesType);
        durationSeries.update(currentTime, durationFunction.calculate(durationSeries.get(currentTime), value, "nextLine", 0, currentTime, durationSeries.index(currentTime), "expression"));
    }

    public Map<String, Series<Long[]>> histo() {
        return seriesMap;
    }

    public Series<Map<Long, StatsDuration>> ladder() {
        return ladderSeries;
    }

    private void updateLadder(long currentTime, long newValue1) {
        Map<Long, StatsDuration> ladderMap = ladderSeries.get(currentTime);
        Map<Long, StatsDuration> newValue = ladderFun.calculate(ladderMap, newValue1, "nextLine", 0, currentTime, 0, "");
        ladderSeries.update(currentTime, newValue);
    }

}
