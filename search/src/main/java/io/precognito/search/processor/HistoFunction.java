package io.precognito.search.processor;

public interface HistoFunction {
    long calculate(long currentValue, String nextLine, long position, long time, String expression);
}
