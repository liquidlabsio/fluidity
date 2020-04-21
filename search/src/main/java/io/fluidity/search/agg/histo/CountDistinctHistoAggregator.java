package io.fluidity.search.agg.histo;

import com.clearspring.analytics.stream.membership.BloomFilter;
import com.clearspring.analytics.stream.membership.Filter;
import io.fluidity.search.Search;

import java.io.InputStream;
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
public class CountDistinctHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.countDistinct()");
    }

    public CountDistinctHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series> processSeries(Collection<Series> collectedSeries) {
        Series count = search.getTimeSeries("distinct", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            count.update(point[0], add(count.get(point[0]), point[1]));
        }));
        return Arrays.asList(count);
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountDistinctHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        Filter filter = new BloomFilter(100, 0.01);
        return (currentValue, newValue, nextLine, position, time, expression) -> {
            boolean present = filter.isPresent(newValue.toString());
            filter.add(newValue.toString());
            if (!present) {
                return currentValue == -1 ? currentValue + 2 : +1;
            } else {
                return currentValue;
            }
        };
    }
}
