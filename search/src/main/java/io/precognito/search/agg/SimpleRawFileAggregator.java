package io.precognito.search.agg;

import io.precognito.search.Search;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splices together raw event files by the timestamp.
 * Prerequisite: They MUST start with UNIX_TIME:{line data}
 */
public class SimpleRawFileAggregator implements EventsAggregator {
    private final Map<String, Scanner> streams;
    private final Search search;
    private boolean splitLine = false;

    public SimpleRawFileAggregator(Map<String, InputStream> streams, Search search) {
        this.streams = streams.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> new Scanner(entry.getValue())));
        this.search = search;
        populateLineMap(this.streams);
    }

    private void populateLineMap(Map<String, Scanner> streams) {
        streams.entrySet().stream().filter(entry -> entry.getValue().hasNextLine())
                .forEach(entry -> nextLines.put(entry.getKey(), split(entry.getValue().nextLine()) ));
    }

    private Map.Entry<Long, String> split(String nextLine) {
        int i = nextLine.indexOf(":");
        if (i == -1) return null;
        try {
            Long time = Long.valueOf(nextLine.substring(0, i));

            String line = splitLine ? nextLine.substring(i + 1, nextLine.length()) : nextLine;
            return new AbstractMap.SimpleEntry(time, line);
        } catch (Exception ne) {
            ne.printStackTrace();
            System.out.println("NFE");
            return null;
        }
    }

    public String[] process() {
        StringBuilder results = new StringBuilder();
        String nextLine = "";
        while ((nextLine = getNextLine(streams)) != null) {
            results.append(nextLine).append("\n");
        }
        return new String[] { "no-histogram-available", results.toString() };
    }

    Map<String, Map.Entry<Long, String>> nextLines = new HashMap<>();
    private String getNextLine(Map<String, Scanner> streams) {
        Map.Entry<String, Map.Entry<Long, String >> nextLine = findNextLine(nextLines);
        if (nextLine == null) {
            return null;
        }
        // Note: 'nextLine' points to a hashmap entry that can be mutated by updates below.
        String result = nextLine.getValue().getValue();
        Map.Entry<Long, String> newStreamLineValue = readNewStreamLine(streams.get(nextLine.getKey()));
        if (newStreamLineValue == null){
            streams.remove(nextLine.getKey()).close();
            nextLines.remove(nextLine.getKey());
        } else {
            nextLines.put(nextLine.getKey(), newStreamLineValue);
        }
        return result;
    }

    private Map.Entry<String, Map.Entry<Long, String>> findNextLine(Map<String, Map.Entry<Long, String>> nextLines) {
        Map.Entry<String, Map.Entry<Long, String>> found = null;
        List<Map.Entry<String, Map.Entry<Long, String>>> collect = nextLines.entrySet().stream().filter(entry -> found == null || entry.getValue().getKey() < found.getValue().getKey()).collect(Collectors.toList());
        if (collect.size() == 0) return null;
        else return collect.iterator().next();
    }

    private Map.Entry<Long, String> readNewStreamLine(Scanner inputStream) {
        if (inputStream.hasNextLine()){
            return split(inputStream.nextLine());
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        this.streams.values().stream().forEach(scanner -> scanner.close());
    }
}
