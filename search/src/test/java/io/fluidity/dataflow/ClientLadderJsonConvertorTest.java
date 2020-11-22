/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 *  file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is
 *   distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.dataflow;

import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLadderJsonConvertorTest {

    @Test
    void toFromJson() {
        TimeSeries<Map<Long, FlowStats>> stats = makeFlowStats();
        ClientLadderJsonConvertor jsonConvertor = new ClientLadderJsonConvertor();
        byte[] jsonBytes = jsonConvertor.toJson(stats);
        TimeSeries<Map<Long, FlowStats>> newTimeSeries = jsonConvertor.fromJson(jsonBytes);
        byte[] checkingJson = jsonConvertor.toJson(newTimeSeries);

        System.out.println("GOT:" + new String(jsonBytes));
        assertEquals(new String(jsonBytes), new String(checkingJson), "Reconciliation failed!");
    }

    @Test
    void toClientJsonArrays() {
        ClientLadderJsonConvertor jsonConvertor = new ClientLadderJsonConvertor();
        TimeSeries<Map<Long, FlowStats>> ladder = makeFlowStats();

        // double check json serdes doesnt break type info (it was!)
        byte[] jsonBytes = jsonConvertor.toJson(ladder);
        TimeSeries<Map<Long, FlowStats>> ladder2 = jsonConvertor.fromJson(jsonBytes);

        String clientJson = jsonConvertor.toClientArrays(ladder2);
        System.out.println(clientJson);
        assertTrue(clientJson.contains("[ [ 0 ], [ [ 111 ] ], [ [ 1 ] ] ]"));
    }

    private TimeSeries<Map<Long, FlowStats>> makeFlowStats() {
        TimeSeries<Map<Long, FlowStats>> timeseries = new TimeSeries("none", "", 1000, 3000, new Series.LongOps());
        FlowStats flowStats = new FlowStats();
        Long[] longs = {100l, 200l};
        FlowInfo flowInfo = new FlowInfo("flowId", List.of("someFile"), new ArrayList<>(Collections.singleton(longs)));
        flowStats.update(flowInfo);
        Map<Long, FlowStats> statsMap = Map.of(111l, flowStats);
        timeseries.update(1000, statsMap);
        return timeseries;
    }
}