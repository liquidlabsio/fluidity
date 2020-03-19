package io.precognito.search.agg.events;

import io.precognito.search.Search;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class LineByLineEventAggregatorTest {

    @Test
    void process() throws Exception {

        Map<String, InputStream> streams = createSteams(asList("file1.txt", "file2.txt"), 10);
        Search search = null;
        LineByLineEventAggregator aggregator = new LineByLineEventAggregator(streams, search);
        String[] processed = aggregator.process(0, 100);
        int totalEvents = Integer.parseInt(processed[0]);

        assertEquals(20, totalEvents);
        String eventsData = processed[1];
        System.out.println(eventsData);
        assertTrue(eventsData != null);
        assertTrue(eventsData.contains("file1.txt,1"));
        assertTrue(eventsData.contains("file1.txt,9"));
        assertTrue(eventsData.contains("file2.txt,1"));
        assertTrue(eventsData.contains("file2.txt,9"));
    }

    @Test
    void processAccordingToTimeStamp() throws Exception {

        Map<String, InputStream> streams = createSteams(asList("file1.txt", "file2.txt"), 10);
        Search search = null;
        LineByLineEventAggregator aggregator = new LineByLineEventAggregator(streams, null);
        String[] processed = aggregator.process(5000, 100);
        int totalEvents = Integer.parseInt(processed[0]);

        assertTrue(totalEvents < 20);
        String eventsData = processed[1];
        System.out.println(eventsData);
        assertTrue(eventsData != null);
        assertFalse(eventsData.contains("file1.txt,1"), "Should have skipped the first events");
        assertTrue(eventsData.contains("file1.txt,9"));
        assertFalse(eventsData.contains("file2.txt,1"), "Should have skipped first events");
        assertTrue(eventsData.contains("file2.txt,9"));
    }

    @Test
    void handlesSingLineFiles() throws Exception {

        Map<String, InputStream> streams = createSteams(asList("file1.txt", "file2.txt"), 1);
        Search search = null;
        LineByLineEventAggregator aggregator = new LineByLineEventAggregator(streams, null);
        String[] processed = aggregator.process(0, 100);
        int totalEvents = Integer.parseInt(processed[0]);

        assertEquals(totalEvents, 2);
        String eventsData = processed[1];
        System.out.println(eventsData);
        assertTrue(eventsData != null);
        assertTrue(eventsData.contains("file1.txt,0"), "Should have 1 line");
        assertTrue(eventsData.contains("file2.txt,0"), "Should have 1 line");
        assertFalse(eventsData.contains("file1.txt,1"), "Should have 1 line");
        assertFalse(eventsData.contains("file2.txt,1"), "Should have 1 line");
    }

    private Map<String, InputStream> createSteams(List<String> filenames, int limit) throws Exception {

        HashMap<String, InputStream> results = new HashMap<>();

        filenames.stream().forEach(filename -> {
            results.put(filename, createInputStream(filename, limit));
        });

        return results;
    }

    private InputStream createInputStream(String filename, int limit) {

        StringBuilder sb = new StringBuilder();
        long time = 1000;
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%d:%s-%s,%d\n", time += 1000, "this is my line of stuff", filename, i));
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}