package io.precognito.search.agg;

import io.precognito.search.Search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splices together raw event files by the timestamp.
 * Prerequisite: They MUST start with UNIX_TIME:Offset:{line data} (see SimpleSearch
 * Returns String[] of {
 *   Histogram json,
 *   Merged event data with prepended meta data, format: {FileIndex:UnixTime:Offset:LineData...}
 * }
 * Where FileIndex is the 'index' into the steams LinkedHashMap
 */
public class SimpleLineByLineAggregator implements EventsAggregator {
    private final Map<String, BufferedReader> streams;
    private final Map<String, Integer> fileLut;
    private final Search search;
    private boolean splitLine = false;

    public SimpleLineByLineAggregator(Map<String, InputStream> streams, Search search) {
        this.streams = streams.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey()
                        , entry -> new BufferedReader(new InputStreamReader(entry.getValue()))
                        )
                );
        this.fileLut = populateLut(streams.keySet());
        this.search = search;
        populateLineMap(this.streams);
    }

    private Map<String, Integer> populateLut(Set<String> dataSourceLut) {
        HashMap<String, Integer> results = new HashMap<>();
        dataSourceLut.stream().forEach(key -> results.put(key, results.size()));
        return results;
    }

    private void populateLineMap(Map<String, BufferedReader> streams) {
        streams.entrySet().stream()
                .forEach(entry -> {
                    try {
                        nextLines.put(entry.getKey(), split(entry.getKey(), entry.getValue().readLine()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private Map.Entry<Long, RecordEntry> split(String streamName, String nextLine) {
        int i = nextLine.indexOf(":");
        if (i == -1) return null;
        Long time = Long.valueOf(nextLine.substring(0, i));
        String line = splitLine ? nextLine.substring(i + 1) : nextLine;
        return new AbstractMap.SimpleEntry(time, new RecordEntry(streamName, time, line));
    }

    @Override
    public String[] process(long fromTime, int limit) throws IOException {
        StringBuilder results = new StringBuilder();
        RecordEntry nextRecord;
        int totalEvents = 0;
        while ((nextRecord = getNextLine(streams)) != null && totalEvents < limit) {
            if (nextRecord.getTime() > fromTime) {
                results.append(fileLut.get(nextRecord.getStreamName()));
                results.append(":");
                results.append(nextRecord.getLine()).append("\n");
                totalEvents++;
            }
        }

        return new String[]{Integer.toString(totalEvents), results.toString()};
    }

    Map<String, Map.Entry<Long, RecordEntry>> nextLines = new HashMap<>();

    /**
     * Searched lines are stored using: timestamp:filepos:data
     *
     * @param streams
     * @return
     * @throws IOException
     */
    private RecordEntry getNextLine(Map<String, BufferedReader> streams) throws IOException {
        Map.Entry<String, Map.Entry<Long, RecordEntry>> nextLine = findNextLine(nextLines);
        if (nextLine == null) {
            return null;
        }
        if (nextLine.getValue() == null) {
            String streamUrl = nextLine.getKey();
            streams.remove(streamUrl).close();
            nextLines.remove(streamUrl);
            return null;
        }


        // Note: 'nextLine' points to a hashmap entry that can be mutated by updates below.
        RecordEntry result = nextLine.getValue().getValue();
        String streamUrl = nextLine.getKey();
        Map.Entry<Long, RecordEntry> newStreamLineValue = readNewStreamLine(streamUrl, streams.get(streamUrl));
        if (newStreamLineValue == null) {
            streams.remove(streamUrl).close();
            nextLines.remove(streamUrl);
        } else {
            nextLines.put(streamUrl, newStreamLineValue);
        }
        return result;
    }

    private Map.Entry<String, Map.Entry<Long, RecordEntry>> findNextLine(Map<String, Map.Entry<Long, RecordEntry>> nextLines) {
        Map.Entry<String, Map.Entry<Long, RecordEntry>> found = null;
        List<Map.Entry<String, Map.Entry<Long, RecordEntry>>> collect = nextLines.entrySet().stream().filter(entry -> found == null || entry.getValue().getKey() < found.getValue().getKey()).collect(Collectors.toList());
        if (collect.size() == 0) return null;
        else return collect.iterator().next();
    }

    private Map.Entry<Long, RecordEntry> readNewStreamLine(String streamName, BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            return split(streamName, line);
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        this.streams.values().forEach(reader -> {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
