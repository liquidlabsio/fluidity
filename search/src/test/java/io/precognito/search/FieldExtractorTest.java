package io.precognito.search;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldExtractorTest {

    @Test
    void matchesKvNamePair() {

        FieldExtractor extractor = new FieldExtractor("*|*|*|field.getKVPair(CPU: )");
        AbstractMap.SimpleEntry<String, Long> keyValue = extractor.getSeriesNameAndValue("someFile", "84092300524:235:648701410117:166:1584091742979 INFO CPU: 5  ");
        assertEquals(new AbstractMap.SimpleEntry<>("CPU:", 5l), keyValue);
    }

    @Test
    void matchesKvNamePairAtEoln() {

        FieldExtractor extractor = new FieldExtractor("*|*|*|field.getKVPair(CPU:)");
        AbstractMap.SimpleEntry<String, Long> keyValue = extractor.getSeriesNameAndValue("someFile", "84092300524:235:648701410117:166:1584091742979 INFO CPU:5");
        assertEquals(new AbstractMap.SimpleEntry<>("CPU:", 5l), keyValue);
    }
}