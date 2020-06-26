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

import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates basic stats against incoming series. this version uses a moving average in the function. When aggregating across multiple sources the data is split to min/max/avg
 */
public class StatsHistoAggregator extends AbstractHistoAggregator<Long> {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.stats()");
    }

    public StatsHistoAggregator(Map<String, StorageInputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series<Long>> processSeries(Collection<Series<Long>> collectedSeries) {

        Series<Long> min = search.getTimeSeries("min", "", search.from, search.to);
        Series<Long> max = search.getTimeSeries("max", "", search.from, search.to);
        Series<Long> avg = search.getTimeSeries("avg", "", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            avg.update(point.getLeft(), avg.get(point.getLeft()) == -1 ? point.getRight() : (avg.get(point.getLeft()) + point.getRight()) / 2);
            min.update(point.getLeft(), min.get(point.getLeft()) == -1 ? point.getRight() : Math.min(min.get(point.getLeft()), point.getRight()));
            max.update(point.getLeft(), avg.get(point.getLeft()) == -1 && point.getRight() == -1 ? 0 : Math.max(max.get(point.getLeft()), point.getRight()));
        }));

        return Arrays.asList(min, max, avg);
    }

    @Override
    public HistoAggregator clone(Map<String, StorageInputStream> inputStreams, Search search) {
        return new StatsHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        HistoFunction<Long, Long> histoFunction = new HistoFunction<Long, Long>() {
            int movingAvgLength = 10;
            LinkedList<Long> values = new LinkedList<>();

            @Override
            public Long calculate(Long currentValue, Long newValue, String nextLine, long bytePosition, long time, int histoIndex, String expression) {
                if (newValue instanceof Long) {
                    Long value = newValue;
                    values.add(value);
                    if (values.size() > movingAvgLength) values.pop();
                    return values.stream().collect(Collectors.summingLong(Long::longValue)) / values.size();
                } else {
                    return -1l;
                }
            }
        };
        return histoFunction;
    }
}
