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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.dataflow.histo.StatsDuration;
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
    void ladderGetCollected() throws JsonProcessingException {
        DataflowHistoCollector dataflowHistoCollector = setupTheHistoCollector();
        Series<Map<Long, StatsDuration>> ladder = dataflowHistoCollector.ladder();
        String ladderString = new ObjectMapper().writeValueAsString(ladder);
        System.out.println(ladderString.replace("},{","},\n{"));
        assertTrue(ladderString.contains("\"right\":{\"0\":{\"min\":3000,\"sum\":3000,\"max\":3000,\"count\":1},\"25\":{\"min\":25,\"sum\":25,\"max\":25,\"count\":1}}}"), "Ladder stats not matching");
    }

    @Test
    void flowStatsGetCollected() throws JsonProcessingException {
        DataflowHistoCollector dataflowHistoCollector = setupTheHistoCollector();
        Map<String, Series<Long[]>> histo = dataflowHistoCollector.histo();
        System.out.println(histo);
        assertTrue(histo.containsKey("totalDuration"));

        Series<Long[]> totalDuration = histo.get("totalDuration");
        assertTrue(new ObjectMapper().writeValueAsString(totalDuration).contains("[40,3500,3540,2]"), "Duration stats is missing");

        assertTrue(histo.containsKey("op2OpLatency"));
        assertTrue(histo.containsKey("maxOpDuration"));
    }

    private DataflowHistoCollector setupTheHistoCollector() {
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
        return dataflowHistoCollector;
    }
}