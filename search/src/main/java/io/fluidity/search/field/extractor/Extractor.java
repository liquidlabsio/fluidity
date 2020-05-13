package io.fluidity.search.field.extractor;

import org.graalvm.collections.Pair;

/**
 * FieldExtractors
 */
public interface Extractor {
    Pair<String, Long> getKeyAndValue(String sourceName, String nextLine);
}
