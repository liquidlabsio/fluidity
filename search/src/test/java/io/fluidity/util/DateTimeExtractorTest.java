package io.fluidity.util;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeExtractorTest {

    @Test
    public void matchedDateTime() {
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("yyyy-MM-dd HH:mm.SS");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, "2020-02-14 13:01.22");
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(14, x.getDate());
    }

    @Test
    public void dateTimefromToken() {
        String data = "{\"$schema\":\"/mediawiki/recentchange/1.0.0\",\"meta\":{\"uri\":\"https://www.wikidata.org/wiki/Q76308340\",\"request_id\":\"517dae92-48b1-4cdf-964a-9e8048d8b3b2\",\"id\":\"85c162d1-64e4-459f-8f38-c66d9c74bced\"," +
                "\"dt\":\"2020-04-09T14:52:14Z\",\"domain\":\"www.wikidata.org\",\"stream\":\"mediawiki.recentchange\",\"topic\":\"eqiad.mediawiki.recentchange\",\"partition\":0,\"offset\":2307408905},\"id\":1192167020,\"type\":\"edit\",\"namespace\":0,\"title\":\"Q76308340\",\"comment\":\"/* wbeditentity-update-languages-short:0||nl */ nl-description, [[User:Edoderoobot/Set-nl-description|python code]] - person\",\"timestamp\":1586443934,\"user\":\"Edoderoobot\",\"bot\":true,\"minor\":false,\"patrolled\":true,\"length\":{\"old\":5632,\"new\":5723},\"revision\":{\"old\":1078782987,\"new\":1153760990},\"server_url\":\"https://www.wikidata.org\",\"server_name\":\"www.wikidata.org\",\"server_script_path\":\"/w\",\"wiki\":\"wikidatawiki\",\"parsedcomment\":\"\u200E<span dir=\\\"auto\\\"><span class=\\\"autocomment\\\">Changed label, description and/or aliases in nl: </span></span> nl-description, <a href=\\\"/wiki/User:Edoderoobot/Set-nl-description\\\" title=\\\"User:Edoderoobot/Set-nl-description\\\">python code</a> - person\"}\n";
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("prefix:[\"dt\":\"] yyyy-MM-dd'T'HH:mm:SS");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, data);
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(52, x.getMinutes());
    }

}