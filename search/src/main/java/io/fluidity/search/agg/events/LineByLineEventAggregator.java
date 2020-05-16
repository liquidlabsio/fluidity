/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.search.agg.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
import org.graalvm.collections.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Splices together raw event files by the timestamp.
 * Prerequisite: They MUST start with UNIX_TIME:Offset:{line data} (see SimpleSearch
 * Returns String[] of {
 * Histogram json,
 * Merged event data with prepended meta data, format: {FileIndex:UnixTime:Offset:LineData...}
 * }
 * Where FileIndex is the 'index' into the steams LinkedHashMap
 */
public class LineByLineEventAggregator implements EventsAggregator {
    private final Map<String, BufferedReader> streams;
    private final Map<String, Integer> fileLut;
    private final Search search;
    private boolean splitLine = false;

    public LineByLineEventAggregator(Map<String, InputStream> streams, Search search) {
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
        HashMap<String, Integer> results = new LinkedHashMap<>();
        dataSourceLut.stream().forEach(key -> results.put(key, results.size()));
        return results;
    }

    private void populateLineMap(Map<String, BufferedReader> streams) {
        streams.entrySet().stream()
                .forEach(entry -> {
                    try {
                        String nextLine = entry.getValue().readLine();
                        if (nextLine != null) {
                            nextLines.put(entry.getKey(), split(entry.getKey(), nextLine));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private Pair<Long, RecordEntry> split(String streamName, String nextLine) {
        int i = nextLine.indexOf(":");
        if (i == -1) return null;
        Long time = Long.valueOf(nextLine.substring(0, i));
        String line = splitLine ? nextLine.substring(i + 1) : nextLine;
        return Pair.create(time, new RecordEntry(streamName, time, line));
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

        return new String[]{Integer.toString(totalEvents), results.toString(), getLutIndexAsStringArray()};
    }

    private String getLutIndexAsStringArray() throws JsonProcessingException {
        List<String> keySet = fileLut.keySet().stream().collect(Collectors.toList());
        return new ObjectMapper().writeValueAsString(keySet);
    }

    Map<String, Pair<Long, RecordEntry>> nextLines = new HashMap<>();

    /**
     * Searched lines are stored using: timestamp:filepos:data
     *
     * @param streams
     * @return
     * @throws IOException
     */
    private RecordEntry getNextLine(Map<String, BufferedReader> streams) throws IOException {
        Map.Entry<String, Pair<Long, RecordEntry>> nextLine = findNextLine(nextLines);
        if (nextLine == null) {
            return null;
        }
        if (nextLine.getValue() == null) {
            String streamUrl = nextLine.getKey();
            streams.remove(streamUrl).close();
            nextLines.remove(streamUrl);
            return getNextLine(streams);
        }


        // Note: 'nextLine' points to a hashmap entry that can be mutated by updates below.
        RecordEntry result = nextLine.getValue().getRight();
        String streamUrl = nextLine.getKey();
        Pair<Long, RecordEntry> newStreamLineValue = readNewStreamLine(streamUrl, streams.get(streamUrl));
        if (newStreamLineValue == null) {
            streams.remove(streamUrl).close();
            nextLines.remove(streamUrl);
        } else {
            nextLines.put(streamUrl, newStreamLineValue);
        }
        return result;
    }

    private Map.Entry<String, Pair<Long, RecordEntry>> findNextLine(Map<String, Pair<Long, RecordEntry>> nextLines) {
        Map.Entry<String, Pair<Long, RecordEntry>> found = null;
        List<Map.Entry<String, Pair<Long, RecordEntry>>> collect = nextLines.entrySet().stream().filter(entry -> found == null || entry.getValue().getLeft() < found.getValue().getLeft()).collect(Collectors.toList());
        if (collect.size() == 0) return null;
        else return collect.iterator().next();
    }

    private Pair<Long, RecordEntry> readNewStreamLine(String streamName, BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            return split(streamName, line);
        }
        return null;
    }

    @Override
    public void close() {
        this.streams.values().forEach(reader -> {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
