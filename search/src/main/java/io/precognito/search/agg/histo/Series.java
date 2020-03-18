package io.precognito.search.agg.histo;

import io.precognito.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 {
 *       name: "Series 1",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     }
 */
public class Series {
    private static final long ONE_HOUR = 60 * 60 * 1000;
    private static final long ONE_DAY =  24 * ONE_HOUR;
    private static final long TEN_MINS = 10 * 60 * 1000;
    private static final long ONE_MINS = 60 * 1000;

    public String name;
    public List<Long[]> data= new ArrayList();
    public long delta = ONE_MINS;

    public Series(){
    }
    public Series(String filename, long from, long to) {
        this.name = filename;
        buildHistogram(DateUtil.floorMin(from), DateUtil.floorMin(to));

    }
    private void buildHistogram(long from, long to) {
        long duration = to - from;
        delta = ONE_MINS / 2;
        if (duration > ONE_HOUR) delta = TEN_MINS;
        if (duration > ONE_DAY) delta = ONE_HOUR / 2;
        if (duration > ONE_DAY * 2) delta = ONE_HOUR;
        if (duration > ONE_DAY * 7) delta = ONE_HOUR * 2;
        if (duration > ONE_DAY * 30) delta = ONE_DAY;
        if (duration > ONE_DAY * 60) delta = ONE_DAY * 2;
        if (duration > ONE_DAY * 120) delta = ONE_DAY * 7;
        for (long time = from; time <= to; time += delta) {
            data.add(new Long[]{time, 0l});
        }
    }
    public long get(long time) {
        int index = index(time);
        if (index < 0 || index > data.size()) return 0;
        return data.get(index)[1];
    }

    public void update(long time, long value) {
        int index = index(time);
        if (index < 0 || index > data.size()) return;
        data.get(index)[1] = value;
    }

    public int index(long time) {
        long timeFromStart = time - data.get(0)[0];
        return (int) (timeFromStart / delta);
    }

    public boolean hasData() {
        return data.stream().filter(item -> item[1] > 0).count() > 0;
    }
}
