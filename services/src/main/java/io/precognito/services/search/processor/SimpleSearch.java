package io.precognito.services.search.processor;

import io.precognito.services.search.Search;

import java.io.InputStream;
import java.io.OutputStream;

public class SimpleSearch implements Processor {
    private InputStream input;
    private OutputStream output;

    @Override
    public void process(Search search, InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;

    }

    @Override
    public void close() throws Exception {
        if (input !=null) input.close();
        if (output !=null) output.close();
    }
}
