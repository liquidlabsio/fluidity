/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Optional;

/**
 *
 * Supports formats as follows
 * - yyyy-MM-dd'T'HH:mm:SS
 * - prefix:[dt":"] yyyy-MM-dd'T'HH:mm:SS
 * - prefix:[timestamp":] LONG
 * - prefix:[timestamp":] LONG_SEC
 *
 * Note: As per the examples:
 * - Single quotes are used to support non-parse characters. - the first entry above shows 'T' being literalized
 * - UnixLong support with MS and S granularity - 3rd and 4th examples
 * - Indexing into a record by using 'prefix'. i.e. json fields can be accessed - 2,3,4 above
 *
 */
public class DateTimeExtractor {


    DateTimeParser parser;

    private String format;
    private String prefix;

    public DateTimeExtractor(String format){
        if (format == null || format.length() == 0 || format.equals("*")) {
            return;
        }
        if (format.startsWith("prefix")) {
            prefix = getPrefix(format);
            this.format = format.substring(format.indexOf("]")+1).trim();
        } else {
            this.format = format;
        }

        if (this.format.equals("LONG")) {
            parser = new LongDateTimeParser();
        }
        if (this.format.equals("LONG_SEC")) {
            parser = new LongSecDateTimeParser();
        }

        /**
         * Handle shitty failure scenarios
         */
        try {
            parser = new JodaDateTimeParser(this.format);
        } catch (Exception e) {
        }
    }

    private String getPrefix(String format) {
        int indexFrom = "prefix[".length() + 1;
        int indexTo = format.indexOf("]");
        return format.substring(indexFrom, indexTo);
    }

    public long getTimeMaybe(long currentTime, long guessTimeInterval, Optional<String> line) {
        if (parser == null) {
            return currentTime + guessTimeInterval;
        }
        if (currentTime == 0) {
            currentTime = System.currentTimeMillis() - DateUtil.HOUR;
        }

        try {
            return parser.parseString(getStringSegment(line.get(), parser.formatLength()));
        } catch (Exception ex) {
            parser = null;
            return currentTime + guessTimeInterval;
        }
    }

    private String getStringSegment(String line, int length) {
        int from = 0;
        int to = from + length;
        if (prefix != null) {
            from = line.indexOf(prefix) + prefix.length();
            to = from + length;
        }
        return line.substring(from, to);
    }

    static class JodaDateTimeParser implements DateTimeParser {

        private final String format;
        private int formatLength;
        transient DateTimeFormatter dateTimeFormatter;

        public JodaDateTimeParser(String format) {
            this.dateTimeFormatter = DateTimeFormat.forPattern(format);
            this.format = format;
            this.formatLength = format.length();
            this.formatLength -= format.chars().filter(ch -> ch == '\'').count();
        }

        @Override
        public int formatLength() {
            return formatLength;
        }

        @Override
        public long parseString(String string) {
            return dateTimeFormatter.parseDateTime(string).getMillis();
        }
    }

    static class LongSecDateTimeParser implements DateTimeParser {
        @Override
        public long parseString(String string) {
            return Long.valueOf(string) * 1000;
        }

        @Override
        public int formatLength() {
            return 10;
        }
    }

    static class LongDateTimeParser implements DateTimeParser {
        @Override
        public long parseString(String string) {
            return Long.valueOf(string);
        }

        @Override
        public int formatLength() {
            return 13;
        }
    }

    interface DateTimeParser {
        long parseString(String string);

        int formatLength();
    }
}
