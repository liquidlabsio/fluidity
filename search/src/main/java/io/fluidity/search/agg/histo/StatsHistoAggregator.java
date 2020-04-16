package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates basic stats against incoming series. this version uses a moving average in the function. When aggregating across multiple sources the data is split to min/max/avg
 */
public class StatsHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.stats()");
    }

    public StatsHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series> processSeries(List<Series> collectedSeries) {

        Series min = search.getTimeSeries("min", search.from, search.to);
        Series max = search.getTimeSeries("max", search.from, search.to);
        Series avg = search.getTimeSeries("avg", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            avg.update(point[0], avg.get(point[0]) == -1 ? point[1] : (avg.get(point[0]) + point[1]) / 2);
            min.update(point[0], min.get(point[0]) == -1 ? point[1] : Math.min(min.get(point[0]), point[1]));
            max.update(point[0], avg.get(point[0]) == -1 && point[1] == -1 ? 0 : Math.max(max.get(point[0]), point[1]));
        }));

        return Arrays.asList(min, max, avg);
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new StatsHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        HistoFunction histoFunction = new HistoFunction() {
            int movingAvgLength = 10;
            LinkedList<Long> values = new LinkedList<>();

            @Override
            public long calculate(long currentValue, long newValue, String nextLine, long position, long time, String expression) {
                values.add(newValue);
                if (values.size() > movingAvgLength) values.pop();
                return values.stream().collect(Collectors.summingLong(Long::longValue)) / values.size();
            }
        };
        return histoFunction;
    }
}
