package io.precognito.search.agg.histo;

public class NoopHistoCollector implements HistoCollector {
    @Override
    public void add(long currentTime, long position, String nextLine) {

    }

    @Override
    public void close() throws Exception {

    }
}
