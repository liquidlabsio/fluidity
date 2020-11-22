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

import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientHistoJsonConvertorTest {

    @Test
    void toFromJson() {
        TimeSeries<FlowStats> stats = makeFlowStats();
        ClientHistoJsonConvertor jsonConvertor = new ClientHistoJsonConvertor();
        byte[] jsonBytes = jsonConvertor.toJson(stats);
        TimeSeries<FlowStats> newTimeSeries = jsonConvertor.fromJson(jsonBytes);
        byte[] checkingJson = jsonConvertor.toJson(newTimeSeries);
        assertEquals(new String(jsonBytes), new String(checkingJson), "Reconciliation failed!");
    }

    @Test
    void toClientJsonArrays() {
        TimeSeries<FlowStats> timeSeries = makeFlowStats();
        String clientJson = new ClientHistoJsonConvertor().toClientArrays(timeSeries);
        System.out.println(clientJson);
        assertTrue(clientJson.contains("[ 100 ]"));
    }

    private TimeSeries<FlowStats> makeFlowStats() {
        TimeSeries<FlowStats> stats = new TimeSeries("none", "", 1000, 3000, new Series.LongOps());
        FlowStats flowStats = new FlowStats();
        Long[] longs = {100l, 200l};
        FlowInfo flowInfo = new FlowInfo("flowId", List.of("someFile"), new ArrayList<>(Collections.singleton(longs)));
        flowStats.update(flowInfo);
        stats.update(1000, flowStats);
        return stats;
    }
}