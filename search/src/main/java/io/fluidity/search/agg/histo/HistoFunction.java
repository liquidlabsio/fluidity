package io.fluidity.search.agg.histo;

public interface HistoFunction {
    long calculate(long currentValue, Object newValue, String nextLine, long position, long time, String expression);
}
