package io.fluidity.dataflow;

import io.fluidity.util.DateUtil;
import org.graalvm.collections.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.fluidity.dataflow.DataflowExtractor.CORR_FILE_FMT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataflowModellerTest {

    @Test
    void getCorrelationFlow() {

        List<Pair<Long, String>> correlationSet = new ArrayList<>();
        long start = System.currentTimeMillis() - DateUtil.HOUR;
        long middle = start + DateUtil.MINUTE * 2;
        long end = start + DateUtil.MINUTE * 5;

        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", "txn123", start, start + DateUtil.MINUTE)));
        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", "txn123", middle, middle + DateUtil.MINUTE)));
        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", "txn123", end, end + DateUtil.MINUTE)));

        FlowInfo correlationFlow = new DataflowModeller().getCorrelationFlow("txn123", correlationSet);
        assertNotNull(correlationFlow);
        assertEquals(6, correlationFlow.durationMs / DateUtil.MINUTE);
        assertEquals(3, correlationFlow.durations.size());
        assertEquals(3, correlationFlow.flowFiles.size());
    }

    @Test
    void getCorrelationFlowStart() {
    }

    @Test
    void getCorrelationFlowEnd() {
    }
}