package io.fluidity.search;

import io.fluidity.search.field.extractor.FieldExtractor;
import org.graalvm.collections.Pair;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldExtractorTest {
    @Test
    void matchesKvJsonPair() {
        String data = "\"timestamp\":1587210348,\"user\":\"99kerob\",\"bot\":false,\"minor\":false,";
        FieldExtractor extractor = new FieldExtractor("*|*|*|field.getJsonPair(user)");
        Pair<String, Object> keyValue = extractor.getSeriesNameAndValue("someFile", data);
        assertEquals(Pair.create("99kerob" , 1), keyValue);
    }

    @Test
    void matchesKvNamePair() {
        FieldExtractor extractor = new FieldExtractor("*|*|*|field.getKVPair(CPU: )");
        Pair<String, Object> keyValue = extractor.getSeriesNameAndValue("someFile", "84092300524:235:648701410117:166:1584091742979 INFO CPU: 5  ");
        assertEquals(Pair.create("CPU:", 5l), keyValue);
    }

    @Test
    void matchesKvNamePairAtEoln() {

        FieldExtractor extractor = new FieldExtractor("*|*|*|field.getKVPair(CPU:)");
        Pair<String, Object> keyValue = extractor.getSeriesNameAndValue("someFile", "84092300524:235:648701410117:166:1584091742979 INFO CPU:5");
        assertEquals(Pair.create("CPU:", 5l), keyValue);
    }
}