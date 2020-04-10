package io.precognito.search.agg.histo;

import io.precognito.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static io.precognito.util.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class OverlayTimeSeriesTest {

    @Test
    public void testStandardTimeSeries() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - HOUR;
        Series series = new OverlayTimeSeries("someFile", from, last);

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
        Series series = new OverlayTimeSeries("someFile", from, last);

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
        Series series = new OverlayTimeSeries("someData", from, last);

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