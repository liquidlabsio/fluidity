package io.precognito.search.processor;

import io.precognito.search.Search;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Processor extends AutoCloseable {
    /**
     * Note: lines must be written to the outputstream using: timestamp:filepos:data
     */
    int process(HistoCollector histoCollector, Search search, InputStream input, OutputStream output, long fromTime, long toTime, long length) throws IOException;
}
