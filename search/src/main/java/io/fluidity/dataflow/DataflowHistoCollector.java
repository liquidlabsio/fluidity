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
import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;

import java.util.Map;

/**
 * Collects aggregate statistics across all dataflows
 */
public class DataflowHistoCollector {
    private LatencyLadderFun ladderFun;
    private Series<Map<Long, FlowStats>> ladderStatsHisto;

    private DurationStatsFun flowFun;
    private TimeSeries<FlowStats> flowStatsHisto;

    public DataflowHistoCollector(Search search) {
        this.flowFun = new DurationStatsFun();
        this.flowStatsHisto = new TimeSeries("none", "", search.from, search.to, new Series.LongOps());

        this.ladderFun =  new LatencyLadderFun();
        this.ladderStatsHisto = new TimeSeries("none", "", search.from, search.to, new Series.LongOps());
    }

    public void add(long currentTime, FlowInfo flowValue) {

        flowStatsHisto.update(currentTime,
                flowFun.calculate(flowStatsHisto.get(currentTime), flowValue, "nextLine", 0,
                        currentTime, flowStatsHisto.index(currentTime), "expression")
        );

        ladderStatsHisto.update(currentTime,
                ladderFun.calculate(ladderStatsHisto.get(currentTime), flowValue, "nextLine", 0,
                        currentTime, 0, "")
        );
    }

    public Series<FlowStats> flowHisto() {
        return flowStatsHisto;
    }
    public Series<Map<Long, FlowStats>> ladderHisto() {
        return ladderStatsHisto;
    }
}
