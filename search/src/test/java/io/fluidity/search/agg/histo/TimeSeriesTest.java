package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesTest {

    @Test
    public void testBuildSeriesAndGetStuff() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - DateUtil.HOUR;
        Series<Long> series = new TimeSeries("someFile", "", from, last);

        assertFalse(series.hasData());

        testBucket("Before From Bucket", from - DateUtil.HOUR, series, true);
        testBucket("From Bucket", from, series, false);
        testBucket("From Bucket+1000", from+1000, series, false);
        testBucket("Middle Bucket", from + 30 * DateUtil.MINUTE, series, false);
        testBucket("Last Bucket", last, series, false);
        testBucket("Last Bucket-1000", last-1000, series, false);
        testBucket("Last Bucket+1000", last+1000, series, false);
        testBucket("Last Bucket+1H", last+DateUtil.HOUR, series, true);

        assertTrue(series.hasData());
    }

    private void testBucket(String message, long time, Series<Long> series, boolean shouldFail) {
        long value = 1234;
        series.update(time, value);
        Long returnedValue = series.get(time);
        if (!shouldFail) {
            assertEquals(value, returnedValue, message);
        }
    }
}