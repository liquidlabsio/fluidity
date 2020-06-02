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

import io.fluidity.search.agg.histo.HistoFunction;
import io.fluidity.util.DateUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies state to each index
 */
public class LatencyLadderFun implements HistoFunction<Map<Long, StatsDuration>, Long> {
    private static final Long ORDINAL_MS_GRANULARITY = 50l;
    Map<Long, Map<Long, StatsDuration>> indexedFuns = new HashMap();

    public LatencyLadderFun() {
    }

    @Override
    public Map<Long, StatsDuration> calculate(Map<Long, StatsDuration> currentValue, Long newValue, String nextLine, long bytePosition, long time, int histoIndex, String expression) {
        // calculate a ladder function - return Long[][] - i.e. count of flows for each ladder using the ordinal value to bucket stats into the ladder
        Map<Long, StatsDuration> ladder = indexedFuns.computeIfAbsent(DateUtil.floorMin(time), k -> new HashMap<>());
        StatsDuration currentStats = ladder.computeIfAbsent((newValue % ORDINAL_MS_GRANULARITY), k -> new StatsDuration());
        currentStats.update(newValue);
        return ladder;
    }
}
