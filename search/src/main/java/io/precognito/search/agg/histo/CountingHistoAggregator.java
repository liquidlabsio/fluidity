package io.precognito.search.agg.histo;

import io.precognito.search.Search;
import io.precognito.search.processor.HistoFunction;
import io.precognito.search.processor.Series;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CountingHistoAggregator extends AbstractHistoAggregator {

    public CountingHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series> processSeries(List<Series> collectedSeries) {

        Series count = new Series("count", search.from, search.to);
        collectedSeries.stream().forEach(series -> series.data.stream().forEach(point -> {
            count.update(point[0], count.get(point[0]) + point[1]);
        }));

        return Arrays.asList(count);
    }

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.count()");
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountingHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        return (currentValue, newValue, nextLine, position, time, expression) -> currentValue + 1;
    }
}
