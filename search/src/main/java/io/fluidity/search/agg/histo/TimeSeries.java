package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

import static io.fluidity.util.DateUtil.*;

/**
 * {
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
public class TimeSeries implements Series {

    public String name;
    public List<Long[]> data = new ArrayList();
    public long delta = DateUtil.MINUTE;

    public TimeSeries() {
    }

    public TimeSeries(String filename, long from, long to) {
        this.name = filename;
        buildHistogram(DateUtil.floorMin(from), DateUtil.floorMin(to));
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
        delta = MINUTE;
        if (duration > HOUR) delta = TEN_MINS;
        if (duration > DAY) delta = HOUR / 2;
        if (duration > DAY * 2) delta = HOUR;
        if (duration > WEEK) delta = HOUR * 2;
        if (duration > WEEK * 4) delta = DAY;
        if (duration > WEEK * 8) delta = DAY * 2;
        if (duration > WEEK * 12) delta = WEEK;
        for (long time = from; time <= to; time += delta) {
            data.add(new Long[]{time, -1l});
        }
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
    public int index(long time) {
        long timeFromStart = time - data.get(0)[0];
        return (int) (timeFromStart / delta);
    }

    @Override
    public boolean hasData() {
        return data.stream().filter(item -> item[1] > 0).count() > 0;
    }

    @Override
    public void merge(Series series) {
        series.data().stream()
                .forEach(dataPoint ->
                        this.update(dataPoint[0], this.get(dataPoint[0]) + dataPoint[1])
                );
    }
}
