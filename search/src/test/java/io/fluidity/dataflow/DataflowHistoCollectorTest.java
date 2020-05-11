package io.fluidity.dataflow;

import io.fluidity.search.Search;
import io.fluidity.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
        String results = dataflowHistoCollector.results();
        System.out.println(results);
        assertTrue(results.contains("totalDuration"));
        assertTrue(results.contains("[40,3500,3540,2]"), "Duration stats is missing");

        assertTrue(results.contains("op2OpLatency"));
        assertTrue(results.contains("maxOpDuration"));
    }
}