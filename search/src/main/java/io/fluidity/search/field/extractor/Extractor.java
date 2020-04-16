package io.fluidity.search.field.extractor;

import java.util.AbstractMap;

/**
 * FieldExtractors
 */
public interface Extractor {
    AbstractMap.SimpleEntry<String, Long> getKeyAndValue(String sourceName, String nextLine);
}
