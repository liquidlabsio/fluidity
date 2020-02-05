package io.precognito.services.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FixturedSearchService implements SearchService {
    private final Logger log = LoggerFactory.getLogger(FixturedSearchService.class);

    static Map<String, byte[]> storage = new HashMap<>();

    public FixturedSearchService() {
        log.info("Created");
    }

    @Override
    public String[] submit(Search search) {
        return new String[] { "s3://bucket/file1.txt", "s3://bucket/file2.txt" };
    }

    @Override
    public String[] searchFile(String[] files, Search search) {
        return new String[] { String.format("s3://staging/%s/%s.raw", search.uid, files[0])};
    }

    @Override
    public String[] finalizeResults(String[] files, Search search) {
        return new String[] { "histo-results", "Have some text results"};
    }
}