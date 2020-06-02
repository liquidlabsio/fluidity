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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CountEachHistoAggregator extends AbstractHistoAggregator<Long> {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.countEach()");
    }

    public static final int LIMIT = 25;

    public CountEachHistoAggregator(Map<String, StorageInputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    @Override
    List<Series<Long>> processSeries(Collection<Series<Long>> collectedSeries) {
        final Set<String> topSeries = getTopSeriesNames(collectedSeries, LIMIT);

        Map<String, Series<Long>> results = new HashMap<>();

        // collect top items
        collectedSeries.stream().filter(series -> topSeries.contains(series.name())).forEach(series -> {
            if (results.containsKey(series.name())) {
                Series<Long> series1 = results.get(series.name());
                series1.merge(series);
            } else {
                results.put(series.name(), series);
            }
        });

        // collect other items
        Series<Long> other = search.getTimeSeries("other", "", search.from, search.to);
        results.put(other.name(), other);

        collectedSeries.stream().filter(series -> !topSeries.contains(series.name())).forEach(series -> other.merge(series));
        return new ArrayList<>(results.values());
    }

    private Set<String> getTopSeriesNames(Collection<Series<Long>> collectedSeries, int limit) {
        // count the total hits for the series
        Map<String, Long> countMap = new HashMap<>();
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(data -> {
            Long aLong = countMap.get(series.name());
            if (aLong == null || aLong == -1) aLong = 0L;
            countMap.put(series.name(), add(aLong, data.getRight()));
        }));
        // sort by number of hits
        List<Map.Entry<String, Long>> entryArrayList = new ArrayList<>(countMap.entrySet());

        entryArrayList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        if (entryArrayList.size() > limit) {
            entryArrayList = entryArrayList.subList(0, limit);
        }

        // strip out the names
        return entryArrayList.stream().map(item -> item.getKey()).collect(Collectors.toSet());
    }

    @Override
    public HistoAggregator clone(Map<String, StorageInputStream> inputStreams, Search search) {
        return new CountEachHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        return (currentValue, newValue, nextLine, bytePosition, time, histoIndex, expression) -> currentValue == null ? 1 : currentValue.longValue() + 1;
    }

    protected long add(Long currentValue, Long newValue) {
        currentValue = currentValue == null ? 0 : currentValue;
        newValue = newValue == null ? 0 : newValue;
        return currentValue.longValue() + newValue.longValue();
    }
}
