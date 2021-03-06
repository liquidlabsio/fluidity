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
import org.graalvm.collections.Pair;

import java.util.*;

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
    private Ops<T> ops = (Ops<T>) new LongOps();
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
        if (duration > 12 * HOUR) delta = MINUTE * 3;
        if (duration > 24 * HOUR) delta = MINUTE * 4;
        if (duration >= DAY * 2) delta = MINUTE * 10;
        if (duration >= DAY * 5) delta = HOUR;
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

    @Override
    public Collection<Series<T>> slice(long timeBucket) {
        HashMap<Long, Series<T>> results = new LinkedHashMap<>();
        this.data.stream().forEach(item -> {
            long seriesStartTime = DateUtil.floorHour(item.getLeft());
            TimeSeries<T> tSeries = (TimeSeries<T>) results.computeIfAbsent(seriesStartTime,
                    k -> new TimeSeries(this.name, this.groupBy, seriesStartTime, seriesStartTime + timeBucket, this.ops));
            tSeries.replace(item);
        });
        return results.values();
    }

    private void replace(Pair<Long, T> item) {
        int index = index(item.getLeft());
        this.data.set(index, item);
    }

    @Override
    public long start() {
        return data.get(0).getLeft();
    }

    @Override
    public long end() {
        return data.get(data.size()-1).getLeft();
    }

    /**
     * Getters/Settings for support bean json serialization
     */
    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Pair<Long, T>> getData() {
        return data;
    }

    public void setData(List<Pair<Long, T>> data) {
        this.data = data;
    }

    public long getDelta() {
        return delta;
    }

    public void setDelta(long delta) {
        this.delta = delta;
    }

    @Override
    public String toString() {
        return "TimeSeries{" +
                "groupBy='" + groupBy + '\'' +
                " start=" + DateUtil.printTime(this.start()) +
                " end=" + DateUtil.printTime(this.end()) +
                ", ops=" + ops +
                ", name='" + name + '\'' +
                ", delta=" + delta +
                '}';
    }
}
