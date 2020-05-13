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
