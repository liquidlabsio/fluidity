package io.fluidity.search.agg.events;

public class RecordEntry {
    private String streamName;
    private final Long time;
    private final String line;

    public RecordEntry(String streamName, Long time, String line) {
        this.streamName = streamName;
        this.time = time;
        this.line = line;
    }

    public String getStreamName() {
        return streamName;
    }

    public long getTime() {
        return time;
    }

    public String getLine() {
        return line;
    }
}
