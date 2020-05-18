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

import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataflowHistoCollectorTest {

    @Test
    void add() {
        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = System.currentTimeMillis() - DateUtil.HOUR;
        search.to = System.currentTimeMillis();
        DataflowHistoCollector dataflowHistoCollector = new DataflowHistoCollector(search);
        long time = search.from + DateUtil.MINUTE;
        dataflowHistoCollector.add(time, new FlowInfo("someFlowId", Arrays.asList("/someFlowFile.log"),
                List.of(new Long[]{10l, 20l}, new Long[]{25l, 50l})));
        dataflowHistoCollector.add(time, new FlowInfo("someFlowId", Arrays.asList("/someFlowFile.log"),
                List.of(new Long[]{1000l, 4000l}, new Long[]{4100l, 4500l})));
        Map<String, Series<Long[]>> results = dataflowHistoCollector.results();
        System.out.println(results);
        assertTrue(results.containsKey("totalDuration"));
//        assertTrue(results.contains("[40,3500,3540,2]"), "Duration stats is missing");
//
        assertTrue(results.containsKey("op2OpLatency"));
//        assertTrue(results.contains("maxOpDuration"));
    }
}