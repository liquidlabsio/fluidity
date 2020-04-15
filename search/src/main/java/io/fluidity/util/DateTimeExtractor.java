package io.fluidity.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateTimeExtractor {

    private boolean invalidFormat = false;
    transient DateTimeFormatter dateTimeFormatter;
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

        /**
         * Handle shitty failure scenarios
         */
        if (dateTimeFormatter == null) {
            try {
                dateTimeFormatter = DateTimeFormat.forPattern(this.format);
            } catch (Exception e) {
            }
        }
    }

    private String getPrefix(String format) {
        int indexFrom = "prefix[".length()+1;
        int indexTo = format.indexOf("]");
        return format.substring(indexFrom, indexTo);
    }

    public long getTimeMaybe(long currentTime, long guessTimeInterval, String line) {
        if (dateTimeFormatter == null ) {
            return currentTime + guessTimeInterval;
        }

        try {
            int from = 0;
            int to = from + format.length();
            if (prefix != null) {
                from = line.indexOf(prefix) + prefix.length();
                to = from + format.length();
                if (format.contains("'")) to -= Long.valueOf(format.chars().filter(ch -> ch == '\'').count()).intValue();
            }
            DateTime dateTime = dateTimeFormatter.parseDateTime(line.substring(from, to));
            return dateTime.getMillis();
        } catch (Exception ex) {
            invalidFormat = true;
            return currentTime + guessTimeInterval;
        }
    }
}
