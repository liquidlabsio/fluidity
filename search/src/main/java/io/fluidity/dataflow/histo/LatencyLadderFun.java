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

package io.fluidity.dataflow.histo;

import io.fluidity.dataflow.FlowInfo;
import io.fluidity.search.agg.histo.HistoFunction;
import io.fluidity.util.DateUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a FLowInfo to a Ladder bucket using the time duration to choose the bucket.
 */
public class LatencyLadderFun implements HistoFunction<Map<Long, FlowStats>, FlowInfo> {
    private static final Long ORDINAL_MS_GRANULARITY = Long.getLong("ladder.granularity", 100l);
    Map<Long, Map<Long, FlowStats>> indexedFuns = new HashMap();

    public LatencyLadderFun() {
    }

    @Override
    public Map<Long, FlowStats> calculate(Map<Long, FlowStats> currentValue, FlowInfo flowInfo, String nextLine, long bytePosition, long time, int histoIndex, String expression) {
        // calculate a ladder function - return Long[][] - i.e. count of flows for each ladder using the ordinal value to bucket stats into the ladder
        Map<Long, FlowStats> ladder = indexedFuns.computeIfAbsent(DateUtil.floorMin(time), k -> new HashMap<>());

        long duration = flowInfo.getDuration();
        FlowStats currentStats = ladder.computeIfAbsent( nearestBucket(duration, ORDINAL_MS_GRANULARITY), k -> new FlowStats());
        currentStats.update(flowInfo);
        return ladder;
    }

    private Long nearestBucket(long duration, Long ordinalMsGranularity) {
        return duration - duration % ordinalMsGranularity;
    }
}
