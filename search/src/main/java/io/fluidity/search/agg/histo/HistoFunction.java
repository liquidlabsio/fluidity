package io.fluidity.search.agg.histo;

public interface HistoFunction {
    long calculate(long currentValue, long newValue, String nextLine, long position, long time, String expression);
}
