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

package io.fluidity.search.agg.histo;

import io.fluidity.util.DateUtil;
import org.graalvm.collections.Pair;

import java.util.ArrayList;
import java.util.List;

import static io.fluidity.util.DateUtil.DAY;
import static io.fluidity.util.DateUtil.HOUR;
import static io.fluidity.util.DateUtil.MINUTE;
import static io.fluidity.util.DateUtil.WEEK;

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
public class TimeSeries<T> implements Series<T> {

    public String groupBy;
    private Ops<T> ops;
    public String name;
    public List<Pair<Long, T>> data = new ArrayList<>();
    public long delta = DateUtil.MINUTE;

    public TimeSeries() {
    }

    public TimeSeries(String filename, String groupBy, long from, long to, Ops<T> ops) {
        this.name = filename;
        this.groupBy = groupBy;
        this.ops = ops;
        buildHistogram(DateUtil.floorMin(from), DateUtil.floorMin(to));
    }

    @Override
    public List<Pair<Long, T>> data() {
        return data;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String groupBy() {
        return groupBy;
    }
    private void buildHistogram(long from, long to) {
        long duration = to - from;
        delta = MINUTE;
        if (duration >= 6 * HOUR) delta = MINUTE * 2;
        if (duration > 12 * HOUR) delta = MINUTE * 2;
        if (duration >= DAY * 2) delta = MINUTE * 5;
        if (duration > WEEK) delta = HOUR * 2;
        if (duration > WEEK * 2) delta = DAY;
        if (duration > WEEK * 8) delta = DAY / 2;
        if (duration > WEEK * 12) delta = WEEK;
        for (long time = from; time <= to; time += delta) {
            data.add(Pair.create(time, null));
        }
    }

    @Override
    public T get(long time) {
        int index = index(time);
        if (index < 0 || index >= data.size()) return null;
        return data.get(index).getRight();
    }

    @Override
    public void update(long time, T value) {
        int index = index(time);
        if (index < 0 || index >= data.size()) return;
        Pair<Long, T> current = data.get(index);
        data.remove(index);
        data.add(index, Pair.create(current.getLeft(), value));
    }

    @Override
    public int index(long time) {
        long timeFromStart = time - data.get(0).getLeft();
        return (int) (timeFromStart / delta);
    }

    @Override
    public boolean hasData() {
        return data.stream().filter(item -> item.getRight() != null).count() > 0;
    }

    @Override
    public void merge(Series<T> series) {
        series.data().stream()
                .forEach(dataPoint ->
                        this.update(dataPoint.getLeft(), ops.add(this.get(dataPoint.getLeft()), dataPoint.getRight()))
                );
    }


}
