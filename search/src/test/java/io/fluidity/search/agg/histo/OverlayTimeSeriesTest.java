package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static io.fluidity.util.DateUtil.HOUR;
import static io.fluidity.util.DateUtil.MINUTE;
import static io.fluidity.util.DateUtil.floorDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayTimeSeriesTest {


    @Test
    public void testReproduceOffsetBug() throws Exception {
        // reproduce
        // events at 12:30
        // use time series overlay at 1430pm projects to 1400-1500 - for 24 hours.
        // should see events at 14:30
        //

        long today = floorDay(System.currentTimeMillis());

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("HH:mm.SS");

        long from = today + dateTimeFormatter.parseDateTime("01:00.00").getMillis();
        long to = today + dateTimeFormatter.parseDateTime("15:45.00").getMillis();
        Series series = new OverlayTimeSeries("someFile", "", from, to);


        DateTime localTime = dateTimeFormatter.parseDateTime("12:30.00");
        long millis = today + localTime.getMillis();

        series.update(millis, 100);

        long getTime = today + dateTimeFormatter.parseDateTime("15:30.00").getMillis();

        long translatedHits = series.get(getTime);

        System.out.println("Got Data:" + series.data());

        List<long[]> found = series.data().stream().filter(dataPair -> dataPair[1] > 0).collect(Collectors.toList());


        assertEquals(1, found.size(), "Didn't get any hits!");

        System.out.println("Got Hits at:" + dateTimeFormatter.print(found.get(0)[0]));

        assertEquals(100, translatedHits);
    }

    @Test
    public void testStandardTimeSeries() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - HOUR;
        Series series = new OverlayTimeSeries("someFile", "", from, last);

        assertFalse(series.hasData());

        testBucket("From Bucket", from, series, false);
        testBucket("From Bucket+1000", from + 1000, series, false);
        testBucket("Middle Bucket", from + 30 * MINUTE, series, false);
        testBucket("Last Bucket", last, series, false);
    }

    @Test
    public void testOutOfBoundsOverlay() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - HOUR;
        Series series = new OverlayTimeSeries("someFile", "", from, last);

        assertFalse(series.hasData());

        // bounds testing
        testBucket("Before From Bucket", from - MINUTE, series, false);
        testBucket("Last Bucket-1000", last - 1000, series, false);
        testBucket("Last Bucket+1000", last + 1000, series, false);

        // dont support bounds in the future - only historic
        testBucket("Last Bucket+1H", last + HOUR, series, true);

        assertTrue(series.hasData());
    }

    @Test
    public void testUpdateMatchesAcrossDifferentBounds() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - HOUR;
        Series series = new OverlayTimeSeries("someData", "", from, last);

        long expectedFrom = DateUtil.ceilHour(last)-HOUR;

        // First bucket
        assertEquals(0, series.index(expectedFrom));

        // First bucket - but 1 span before - it should map to the first bucket
        long time = expectedFrom - HOUR + 10;
        assertEquals(0, series.index(time));
    }


    private void testBucket(String message, long time, Series series, boolean shouldFail) {
        long value = 1234;
        series.update(time, value);
        long returnedValue = series.get(time);
        if (shouldFail) {
            assertEquals(0, returnedValue, message + " OutOfIndexBuckets default to 0");
        } else {
            assertEquals(value, returnedValue, message);
        }
    }
}