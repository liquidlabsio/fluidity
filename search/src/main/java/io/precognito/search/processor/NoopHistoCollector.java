package io.precognito.search.processor;

public class NoopHistoCollector implements HistoCollector {
    @Override
    public void add(long currentTime, long position, String nextLine) {

    }

    @Override
    public void close() throws Exception {

    }
}
