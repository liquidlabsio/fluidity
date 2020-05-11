package io.fluidity.services.dataflow.histo;

import io.fluidity.dataflow.FlowInfo;
import io.fluidity.search.agg.histo.HistoFunction;

import java.util.HashMap;
import java.util.Map;

public class HistoAggregatorFun implements HistoFunction<Long, FlowInfo> {
    public static final String TOTAL_DURATION = "totalDuration";
    public static final String OP_2_OP_LATENCY = "op2OpLatency";
    public static final String MAX_OP_DURATION = "maxOpDuration";
    int count = 0;
    // for given index
    // emit client value
    Map<Long, Map<String, StatsDuration>> indexedFuns = new HashMap();

    public HistoAggregatorFun() {
    }

    @Override
    public Long calculate(FlowInfo currentValue, FlowInfo newValue, String nextLine, long position, long time, int histoIndex, String expression) {
        Map<String, StatsDuration> currentStats = indexedFuns.computeIfAbsent(position, k -> createSet());
        count++;
        long duration = newValue.getDuration();
        currentStats.get(TOTAL_DURATION).update(duration);

        long[] minMaxOpIntervalWithOpDuration = newValue.getMinOp2OpLatency();
        currentStats.get(OP_2_OP_LATENCY).update(minMaxOpIntervalWithOpDuration[1]);

        currentStats.get(MAX_OP_DURATION).update(minMaxOpIntervalWithOpDuration[2]);
        return 0l;
    }

    private Map<String, StatsDuration> createSet() {
        return Map.of(TOTAL_DURATION, new StatsDuration(), OP_2_OP_LATENCY, new StatsDuration(), MAX_OP_DURATION, new StatsDuration());
    }
}
