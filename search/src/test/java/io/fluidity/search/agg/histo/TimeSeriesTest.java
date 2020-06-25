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

package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSeriesTest {

    @Test
    public void testSlice() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - (DateUtil.HOUR * 5);
        Series<Long> series = new TimeSeries("someFile", "", from, last, new Series.LongOps());

        series.update(from, 100l);
        series.update(from+DateUtil.HOUR, 200l);
        series.update(from+DateUtil.HOUR*2, 300l);
        series.update(from+DateUtil.HOUR*3, 400l);

        Collection<Series<Long>> sliced = series.slice(DateUtil.HOUR);

        System.out.println("Got:" + sliced.toString().replace(", T", ",\n T"));

        assertTrue(series.hasData());
    }

    @Test
    public void testBuildSeriesAndGetStuff() throws Exception {
        long last = System.currentTimeMillis();
        long from = last - DateUtil.HOUR;
        Series<Long> series = new TimeSeries("someFile", "", from, last, new Series.LongOps());

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