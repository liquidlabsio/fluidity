package io.precognito.services.search.processor;

import io.precognito.services.search.Search;

import java.io.InputStream;
import java.io.OutputStream;

public interface Processor extends AutoCloseable {
    void process(Search search, InputStream input, OutputStream output);
}
