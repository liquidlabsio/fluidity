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

public class CountHistoAggregator extends AbstractHistoAggregator<Long> {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.count()");
    }

    public CountHistoAggregator(Map<String, StorageInputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series<Long>> processSeries(Collection<Series<Long>> collectedSeries) {
        Map<String, Series<Long>> results = new HashMap<>();
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            String groupBy = series.groupBy();
            if (groupBy.length() == 0) groupBy = "count";
            Series<Long> series1 = results.computeIfAbsent(groupBy, k -> search.getTimeSeries(k, "", search.from, search.to));
            series1.update(point.getLeft(), add(series1.get(point.getLeft()), point.getRight()));
        }));
        return new ArrayList<>(results.values());
    }

    protected long add(Long currentValue, Long newValue) {
        currentValue = currentValue == null ? 0 : currentValue;
        newValue = newValue == null ? 0 : newValue;
        return currentValue.longValue() + newValue.longValue();
    }

    @Override
    public HistoAggregator clone(Map<String, StorageInputStream> inputStreams, Search search) {
        return new CountHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        return (currentValue, newValue, nextLine, bytePosition, time, histoIndex, expression) -> currentValue == null ? 1l : currentValue.longValue() + 1l;
    }
}
