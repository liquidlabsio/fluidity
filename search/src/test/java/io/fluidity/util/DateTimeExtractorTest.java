/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.util;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeExtractorTest {

    @Test
    public void matchedDateTimeTest() {
        String testMe = "2020-04-28T19:06.38.775Z START RequestId: a788fb69-f6c4-4ee2-bb40-0c87bc61738c Version: $LATEST";
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("yyyy-MM-dd'T'HH:mm.SS");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, Optional.of(testMe));
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(6, x.getMinutes());
    }

    @Test
    public void matchedDateTime() {
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("yyyy-MM-dd HH:mm.SS");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, Optional.of("2020-02-14 13:01.22"));
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(14, x.getDate());
    }

    @Test
    public void dateTimefromPrefix() {
        String data = "{\"$schema\":\"/mediawiki/recentchange/1.0.0\",\"meta\":{\"uri\":\"https://www.wikidata.org/wiki/Q76308340\",\"request_id\":\"517dae92-48b1-4cdf-964a-9e8048d8b3b2\",\"id\":\"85c162d1-64e4-459f-8f38-c66d9c74bced\"," +
                "\"dt\":\"2020-04-09T14:52:14Z\",\"domain\":\"www.wikidata.org\",\"stream\":\"mediawiki.recentchange\",\"topic\":\"eqiad.mediawiki.recentchange\",\"partition\":0,\"offset\":2307408905},\"id\":1192167020,\"type\":\"edit\",\"namespace\":0,\"title\":\"Q76308340\",\"comment\":\"/* wbeditentity-update-languages-short:0||nl */ nl-description, [[User:Edoderoobot/Set-nl-description|python code]] - person\",\"timestamp\":1586443934,\"user\":\"Edoderoobot\",\"bot\":true,\"minor\":false,\"patrolled\":true,\"length\":{\"old\":5632,\"new\":5723},\"revision\":{\"old\":1078782987,\"new\":1153760990},\"server_url\":\"https://www.wikidata.org\",\"server_name\":\"www.wikidata.org\",\"server_script_path\":\"/w\",\"wiki\":\"wikidatawiki\",\"parsedcomment\":\"\u200E<span dir=\\\"auto\\\"><span class=\\\"autocomment\\\">Changed label, description and/or aliases in nl: </span></span> nl-description, <a href=\\\"/wiki/User:Edoderoobot/Set-nl-description\\\" title=\\\"User:Edoderoobot/Set-nl-description\\\">python code</a> - person\"}\n";
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("prefix:[\"dt\":\"] yyyy-MM-dd'T'HH:mm:SS");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, Optional.of(data));
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(52, x.getMinutes());
    }

    @Test
    public void dateTimeLongfromPrefix() {
        String data = "{\"$schema\":\"/mediawiki/recentchange/1.0.0\",\"meta\":{\"uri\":\"https://www.wikidata.org/wiki/Q76308340\",\"request_id\":\"517dae92-48b1-4cdf-964a-9e8048d8b3b2\",\"id\":\"85c162d1-64e4-459f-8f38-c66d9c74bced\"," +
                "\"dt\":\"2020-04-09T14:52:14Z\",\"domain\":\"www.wikidata.org\",\"stream\":\"mediawiki.recentchange\",\"topic\":\"eqiad.mediawiki.recentchange\",\"partition\":0,\"offset\":2307408905},\"id\":1192167020,\"type\":\"edit\",\"namespace\":0,\"title\":\"Q76308340\",\"comment\":\"/* wbeditentity-update-languages-short:0||nl */ nl-description, [[User:Edoderoobot/Set-nl-description|python code]] - person\",\"timestamp\":1586443934,\"user\":\"Edoderoobot\",\"bot\":true,\"minor\":false,\"patrolled\":true,\"length\":{\"old\":5632,\"new\":5723},\"revision\":{\"old\":1078782987,\"new\":1153760990},\"server_url\":\"https://www.wikidata.org\",\"server_name\":\"www.wikidata.org\",\"server_script_path\":\"/w\",\"wiki\":\"wikidatawiki\",\"parsedcomment\":\"\u200E<span dir=\\\"auto\\\"><span class=\\\"autocomment\\\">Changed label, description and/or aliases in nl: </span></span> nl-description, <a href=\\\"/wiki/User:Edoderoobot/Set-nl-description\\\" title=\\\"User:Edoderoobot/Set-nl-description\\\">python code</a> - person\"}\n";
        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor("prefix:[\"timestamp\":] LONG_SEC");
        long timeMaybe = dateTimeExtractor.getTimeMaybe(System.currentTimeMillis(), 100, Optional.of(data));

        assertEquals(1586443934000l, timeMaybe);
        System.out.println(timeMaybe);
        Date x = new Date(timeMaybe);
        System.out.println(x);
        assertEquals(52, x.getMinutes());
    }


}