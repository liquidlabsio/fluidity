package io.precognito.search.agg;

import java.io.IOException;

public interface EventsAggregator extends AutoCloseable {
    /**
     * Returns totalEvents and Processed Results
     * @return
     * @throws IOException
     */
    String[] process() throws IOException;
}
