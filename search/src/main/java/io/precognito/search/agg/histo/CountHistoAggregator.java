package io.precognito.search.agg.histo;

import io.precognito.search.Search;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CountHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.count()");
    }

    public CountHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series> processSeries(List<Series> collectedSeries) {
        Series count = search.getTimeSeries("count", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            count.update(point[0], count.get(point[0]) + point[1]);
        }));
        return Arrays.asList(count);
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        return (currentValue, newValue, nextLine, position, time, expression) -> currentValue + 1;
    }
}
