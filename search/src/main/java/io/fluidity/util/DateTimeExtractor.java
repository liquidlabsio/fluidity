/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.charset.StandardCharsets;

/**
 *
 * Supports formats as follows
 * - yyyy-MM-dd'T'HH:mm:ss
 * - prefix:[dt":"] yyyy-MM-dd'T'HH:mm:ss
 * - prefix:[timestamp":] LONG
 * - prefix:[timestamp":] LONG_SEC
 *
 * Auto detect ISO formats from data starting with: 2020-11-24T13:31:47.313Z by looking at lines that
 * have char[0] == 2 char[1]=1 char[5]='-' etc
 * See https://en.wikipedia.org/wiki/ISO_8601
 *
 * Note: As per the examples:
 * - Single quotes are used to support non-parse characters. - the first entry above shows 'T' being literalized
 * - UnixLong support with MS and S granularity - 3rd and 4th examples
 * - Indexing into a record by using 'prefix'. i.e. json fields can be accessed - 2,3,4 above
 *
 */
public class DateTimeExtractor {

    private static final String ISO_TIME_FORMAT_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final String JSON_ISO_TIME_PREFIX = "timestamp\":\"";

    private DateTimeParser parser;

    private String format;
    private String prefix;

    public DateTimeExtractor(final String format){
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
            this.parser = new LongDateTimeParser();
        }
        if (this.format.equals("LONG_SEC")) {
            this.parser = new LongSecDateTimeParser();
        }

        /**
         * Handle shitty failure scenarios
         */
        try {
            this.parser = new JodaDateTimeParser(this.format);
        } catch (Exception e) {
        }
    }

    private String getPrefix(final String format) {
        final int indexFrom = "prefix[".length() + 1;
        final int indexTo = format.indexOf("]");
        return format.substring(indexFrom, indexTo);
    }

    public long getTimeMaybe(final long currentTime, final long guessTimeInterval, final String line) {
        if (line.length() == 0) {
            return currentTime;
        }
        if (parser == null) {
            if (isIsoTime(line)) {
                parser = new JodaDateTimeParser(ISO_TIME_FORMAT_MILLIS);
            } else if (isJsonIsoTime(line)) {
                    prefix = JSON_ISO_TIME_PREFIX;
                    parser = new JodaDateTimeParser(ISO_TIME_FORMAT_MILLIS);
                } else {
                return currentTime + guessTimeInterval;
            }
        }

        try {
            return parser.parseString(getStringSegment(line, parser.formatLength()));
        } catch (Exception ex) {
            parser = null;
            return currentTime + guessTimeInterval;
        }
    }

    private boolean isJsonIsoTime(final String lineToCheck) {
        final int offset = lineToCheck.indexOf(JSON_ISO_TIME_PREFIX);
        if (offset != -1) {
            return isIsoTime(lineToCheck.substring(offset+JSON_ISO_TIME_PREFIX.length()));
        }
        return false;
    }

    private static final byte[] sampleLine = "2020-11-24T13:31:47.313Z".getBytes(StandardCharsets.UTF_8);
    private boolean isIsoTime(final String lineToCheck) {
        final byte[] bytes = lineToCheck.getBytes(StandardCharsets.UTF_8);
        return bytes[0] == sampleLine[0] &&
                bytes[1] == sampleLine[1] &&
                bytes[4] == sampleLine[4] &&
                bytes[7] == sampleLine[7] &&
                bytes[10] == sampleLine[10] && // T
                bytes[13] == sampleLine[13] && // :
                bytes[16] == sampleLine[16] && // :
                bytes[19] == sampleLine[19] && // .
                bytes[23] == sampleLine[23]; // Z
    }

    private String getStringSegment(final String line, final int length) {
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

        JodaDateTimeParser(final String format) {
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
        public long parseString(final String string) {
            return dateTimeFormatter.parseDateTime(string).getMillis();
        }
    }

    static class LongSecDateTimeParser implements DateTimeParser {
        @Override
        public long parseString(final String string) {
            return Long.parseLong(string) * 1000L;
        }

        @Override
        public int formatLength() {
            return 10;
        }
    }

    static class LongDateTimeParser implements DateTimeParser {
        @Override
        public long parseString(final String string) {
            return Long.parseLong(string);
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
