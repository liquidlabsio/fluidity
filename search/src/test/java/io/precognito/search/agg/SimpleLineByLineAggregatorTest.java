package io.precognito.search.agg;

import io.precognito.search.Search;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class SimpleLineByLineAggregatorTest {

    @Test
    void process() throws Exception {

        Map<String, InputStream> streams = createSteams(asList( "file1.txt", "file2.txt"));
        Search search = null;
        SimpleLineByLineAggregator aggregator = new SimpleLineByLineAggregator(streams, null);
        String[] processed = aggregator.process();
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

    private Map<String, InputStream> createSteams(List<String> filenames) throws Exception {

        HashMap<String, InputStream> results = new HashMap<>();

        filenames.stream().forEach(filename -> {
            try {
                results.put(filename, createInputStream(filename));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        return results;
    }

    private InputStream createInputStream(String filename) throws InterruptedException {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <10; i++) {
            sb.append(String.format("%d:%s-%s,%d\n", System.currentTimeMillis(), "this is my line of stuff",filename, i));
            Thread.sleep(10);
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}