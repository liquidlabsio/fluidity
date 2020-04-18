package io.fluidity.search.field.extractor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KvJsonPairExtractorTest {

    @Test
    void testStringExtraction() {
        String data = "\"timestamp\":1587210348,\"user\":\"99kerob\",\"bot\":false,\"minor\":false,";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("user");
        AbstractMap.SimpleEntry<String, Object> pair = extractor.getKeyAndValue("pair", data);
        assertEquals("99kerob", pair.getKey());
    }

    @Test
    void testLongExtraction() {
        String data = ",\"user\":\"99kerob\",\"minor\":100,\"";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("minor");
        AbstractMap.SimpleEntry<String, Object> pair = extractor.getKeyAndValue("pair", data);
        System.out.println("Got: " + pair);
        assertEquals(Long.valueOf(100), pair.getValue());
    }

    @Test
    void testDecimalExtraction() {
        String data = ",\"user\":\"99kerob\",\"minor\":100.123 ,\"";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("minor");
        AbstractMap.SimpleEntry<String, Object> pair = extractor.getKeyAndValue("pair", data);
        System.out.println("Got: " + pair);
        assertEquals(Double.valueOf(100.123), pair.getValue());
    }


}