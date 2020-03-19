package io.precognito.search.agg.events;

import io.precognito.search.Search;
import io.precognito.search.agg.histo.HistoCollector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface EventCollector extends AutoCloseable {
    /**
     * Note: lines must be written to an .evt (event) outputstream using: timestamp:filepos:data
     */
    int process(boolean isCompressed, HistoCollector histoCollector, Search search, InputStream input, OutputStream output, long fileFromTime, long fileToTime, long length, String timeFormat) throws IOException;
}
