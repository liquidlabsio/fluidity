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

import io.fluidity.search.Search;

import java.io.InputStream;
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

    public CountEachHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
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
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountEachHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        return (currentValue, newValue, nextLine, position, time, histoIndex, expression) -> currentValue == null ? 1 : currentValue.longValue() + 1;
    }

    protected long add(Long currentValue, Long newValue) {
        currentValue = currentValue == null ? 0 : currentValue;
        newValue = newValue == null ? 0 : newValue;
        return currentValue.longValue() + newValue.longValue();
    }
}
