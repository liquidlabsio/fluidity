package io.precognito.search.processor;

public interface HistoFunction {
    long calculate(long currentValue, long newValue, String nextLine, long position, long time, String expression);
}
