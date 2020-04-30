package io.fluidity.search.field.extractor;

import org.graalvm.collections.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KvJsonPairExtractorTest {


    @Test
    void testStringExtractionAgain() {
        String data = "{\"$schema\":\"/mediawiki/recentchange/1.0.0\",\"meta\":{\"uri\":\"https://www.wikidata.org/wiki/Q91011757\",\"request_id\":\"a540f248-7c19-414d-9286-6047bb824804\",\"id\":\"9ef94a79-e4cb-4da7-8998-fa95d31f1bbc\",\"dt\":\"2020-04-18T12:04:50Z\",\"domain\":\"www.wikidata.org\",\"stream\":\"mediawiki.recentchange\",\"topic\":\"eqiad.mediawiki.recentchange\",\"partition\":0,\"offset\":2331776492},\"id\":1198791222,\"type\":\"new\",\"namespace\":0,\"title\":\"Q91011757\",\"comment\":\"/* wbeditentity-create-item:0| */ batch import from [[Q654724|SIMBAD]] for object \\\"LEDA 439422\\\"\",\"timestamp\":1587211490,\"user\":\"Ghuron\",\"bot\":false,\"minor\":false,\"patrolled\":true,\"length\":{\"new\":5677},\"revision\":{\"new\":1160341353},\"server_url\":\"https://www.wikidata.org\",\"server_name\":\"www.wikidata.org\",\"server_script_path\":\"/w\"" +
                ",\"wiki\":\"wikidatawiki\",\"parsedcomment\":\"\u200E<span dir=\\\"auto\\\"><span class=\\\"autocomment\\\">Создан новый элемент: </span></span> batch import from <a href=\\\"/wiki/Q654724\\\" title=\\\"Q654724\\\">SIMBAD</a> for object &quot;LEDA 439422&quot;\"}";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("wiki");
        Pair<String, Object> pair = extractor.getKeyAndValue("pair", data);
        assertEquals("wikidatawiki", pair.getLeft());
    }
    @Test
    void testStringExtraction() {
        String data = "\"timestamp\":1587210348,\"user\":\"99kerob\",\"bot\":false,\"minor\":false,";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("user");
        Pair<String, Object> pair = extractor.getKeyAndValue("pair", data);
        assertEquals("99kerob", pair.getLeft());
    }

    @Test
    void testLongExtraction() {
        String data = ",\"user\":\"99kerob\",\"minor\":100,\"";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("minor");
        Pair<String, Object> pair = extractor.getKeyAndValue("pair", data);
        System.out.println("Got: " + pair);
        assertEquals(Long.valueOf(100), pair.getRight());
    }

    @Test
    void testDecimalExtraction() {
        String data = ",\"user\":\"99kerob\",\"minor\":100.123 ,\"";
        KvJsonPairExtractor extractor = new KvJsonPairExtractor("minor");
        Pair<String, Object> pair = extractor.getKeyAndValue("pair", data);
        System.out.println("Got: " + pair);
        assertEquals(Double.valueOf(100.123), pair.getRight());
    }


}