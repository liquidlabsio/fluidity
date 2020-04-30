package io.fluidity.search.field.extractor;

import org.graalvm.collections.Pair;

/**
 * FieldExtractors
 */
public interface Extractor {
    Pair<String, Object> getKeyAndValue(String sourceName, String nextLine);
}
