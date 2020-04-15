package io.fluidity.search.agg.events;

import java.io.IOException;

public interface EventsAggregator extends AutoCloseable {
    /**
     * Returns totalEvents and Processed Results
     *
     * @return
     * @throws IOException
     */
    String[] process(long fromTime, int limit) throws IOException;
}
