package io.fluidity.search.agg.events;

import io.fluidity.search.Search;

import java.io.IOException;

public interface EventCollector extends AutoCloseable {
    /**
     * Note: lines must be written to an .evt (event) outputstream using: timestamp:filepos:data
     */
    int[] process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long length, String timeFormat) throws IOException;
}
