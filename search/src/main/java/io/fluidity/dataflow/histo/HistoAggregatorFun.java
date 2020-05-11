package io.fluidity.dataflow.histo;

import io.fluidity.search.agg.histo.HistoFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies state to each index
 */
public class HistoAggregatorFun implements HistoFunction<Long[], Long> {
    Map<Long, StatsDuration> indexedFuns = new HashMap();

    public HistoAggregatorFun() {
    }

    @Override
    public Long[] calculate(Long[] currentValue, Long newValue, String nextLine, long position, long time, int histoIndex, String expression) {
        StatsDuration currentStats = indexedFuns.computeIfAbsent(position, k -> new StatsDuration());
        currentStats.update(newValue);
        return currentStats.data();
    }
}
