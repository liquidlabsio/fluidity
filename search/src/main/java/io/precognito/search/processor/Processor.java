package io.precognito.search.processor;

import io.precognito.search.Search;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Processor extends AutoCloseable {
    int process(Search search, InputStream input, OutputStream output, long fromTime, long toTime, long length) throws IOException;
}
