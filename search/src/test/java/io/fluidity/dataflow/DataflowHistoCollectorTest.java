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
import io.fluidity.dataflow.histo.FlowStats;
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
        Series<Map<Long, FlowStats>> ladder = dataflowHistoCollector.ladderHisto();
        String ladderString = new ObjectMapper().writeValueAsString(ladder);
        System.out.println(ladderString.replace("},{","},\n{"));
        String ladderJson = "{\"0\":{\"opLatency\":[5,5,5],\"opDuration\":[25,25,25],\"duration\":[40,40,40],\"count\":1},\"3500\":{\"opLatency\":[100,100,100],\"opDuration\":[3000,3000,3000],\"duration\":[3500,3500,3500],\"count\":1}";
        assertTrue(ladderString.contains(ladderJson), "Ladder stats not matching");
    }

    @Test
    void flowStatsGetCollected() throws JsonProcessingException {
        DataflowHistoCollector dataflowHistoCollector = setupTheHistoCollector();
        Series<FlowStats> histo = dataflowHistoCollector.flowHisto();
        String histoStats = new ObjectMapper().writeValueAsString(histo);

//        System.out.println(histoStats.replace("},{", "},\n{"));
        assertTrue(histoStats.contains("\"right\":{\"opLatency\":[5,5,105],\"opDuration\":[25,25,3025],\"duration\":[40,40,3540],\"count\":2}}"));
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