package io.precognito.search.agg;

import java.io.IOException;

public interface EventsAggregator extends AutoCloseable {
    String[] process() throws IOException;
}
