package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.fluidity.util.DateUtil.*;

/**
 * {
 * Provides a timeseries overlay with different intervals over the previous periods
 * <p>
 * It currently uses a generic rule to choose the overlay rule (but will become exposed as part of the search expression)
 * Rules:
 * - 1-7 days uses a 1-hour overlay with 1 minute granularity- i.e. all event are mapping into the current-rounded hour (i.e. 6-7pm) and the minute
 * - 7-30 days uses 1-day overlay with 10minute buckets
 * - 30+days uses 7 day overlay with 1 hour buckets
 * name: "Series 1",
 * data: [
 * [1486684800000, 34],
 * [1486771200000, 43],
 * [1486857600000, 31] ,
 * [1486944000000, 43],
 * [1487030400000, 33],
 * [1487116800000, 52]
 * ]
 * }
 */
public class OverlayTimeSeries implements Series {

    public String name;
    public List<Long[]> data = new ArrayList();
    public long delta = MINUTE;
    private long from;
    private long to;
    private long duration;

    public OverlayTimeSeries() {
    }

    public OverlayTimeSeries(String seriesName, long from, long to) {
        this.name = seriesName;
        buildHistogram(from, to);
    }

    @Override
    public List<Long[]> data() {
        return data;
    }

    @Override
    public String name() {
        return name;
    }

    private void buildHistogram(long from, long to) {


        long duration = to - from;

        if (duration < DAY) {
            // < 1-day == 1 hour/minute
            delta = MINUTE;
            this.from = DateUtil.floorHour(to);
            this.to = DateUtil.ceilHour(to);
        } else if (duration < 7 * DAY) {
            // 1-7 days == 1 hour overlay by minute
            delta = MINUTE;
            this.from = DateUtil.floorHour(to);
            this.to = DateUtil.ceilHour(to);
        } else if (duration < DAY * 30) {
            // 7-30 days 1 day overlay by hour
            delta = HOUR;
            this.from = DateUtil.floorDay(to);
            this.to = DateUtil.ceilDay(to);
        } else {
            delta = DAY;
            this.from = DateUtil.floorDay(to);
            this.to = DateUtil.ceilDay(to);
        }

        for (long time = this.from; time <= this.to; time += delta) {
            data.add(new Long[]{time, -1l});
        }
        this.duration = to - from;
    }

    @Override
    public long get(long time) {
        int index = index(time);
        if (index < 0 || index >= data.size()) return 0;
        return data.get(index)[1];
    }

    @Override
    public void update(long time, long value) {
        int index = index(time);
        if (index < 0 || index >= data.size()) return;
        data.get(index)[1] = value;
    }

    @Override
    public String toString() {
        return "OverlayTimeSeries{" +
                "name='" + name + '\'' +
                ", delta=" + delta +
                ", from=" + new Date(from) +
                ", to=" + new Date(to) +
                ", duration=" + duration +
                '}';
    }

//    @Override
//    public int index(long time) {
//        long timeFromStart = time - from;
//        return (int) (timeFromStart / delta);
//    }


    @Override
    public int index(long time) {
        if (time < this.from) {
            // calculate how many factors prior to the window the request is
            long requestDelta = ((from - time) / this.duration) + 1;
            // translate back to an overlay so it looks like a normal time (i.e. index-0 is still index 0 bt yesterdays version)
            long translatedFromTime = from - (requestDelta * this.duration);
            long translatedTimeRequest = time - translatedFromTime;
            return (int) (translatedTimeRequest / delta);
        } else {
            long timeFromStart = time - from;
            return (int) (timeFromStart / delta);
        }
    }

    @Override
    public boolean hasData() {
        return data.stream().filter(item -> item[1] > 0).count() > 0;
    }


}
