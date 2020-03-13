package io.precognito.search;

import java.util.AbstractMap;

public interface Extractor {
    AbstractMap.SimpleEntry<String, Long> getKeyAndValue(String sourceName, String nextLine);
}
