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

import com.clearspring.analytics.stream.membership.BloomFilter;
import com.clearspring.analytics.stream.membership.Filter;
import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Massive shortcomings - will only calculate cardinality against a single source
 * Fix:
 * 1. Series.data[2] value needs to be an Object instead of a long value.
 * 2. Change to use HyperLogLog
 */
public class CountDistinctHistoAggregator extends AbstractHistoAggregator<Long> {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.countDistinct()");
    }

    public CountDistinctHistoAggregator(Map<String, StorageInputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series<Long>> processSeries(Collection<Series<Long>> collectedSeries) {
        Series<Long> count = search.getTimeSeries("distinct", "", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            count.update(point.getLeft(), add(count.get(point.getLeft()), point.getRight()));
        }));
        return Arrays.asList(count);
    }

    @Override
    public HistoAggregator clone(Map<String, StorageInputStream> inputStreams, Search search) {
        return new CountDistinctHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        Filter filter = new BloomFilter(100, 0.01);
        return (currentValue, newValue, nextLine, position, time, histoIndex, expression) -> {
            boolean present = filter.isPresent(newValue.toString());
            filter.add(newValue.toString());
            if (!present) {
                return currentValue == null ? currentValue + 2 : +1;
            } else {
                return currentValue;
            }
        };
    }

    protected long add(Long currentValue, Long newValue) {
        currentValue = currentValue == null ? 0 : currentValue;
        newValue = newValue == null ? 0 : newValue;
        return currentValue.longValue() + newValue.longValue();
    }
}
