package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.*;

public class CountHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.count()");
    }

    public CountHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    List<Series> processSeries(Collection<Series> collectedSeries) {
        Map<String, Series> results = new HashMap<>();
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(point -> {
            String groupBy = series.groupBy();
            if (groupBy.length() == 0) groupBy = "count";
            Series series1 = results.computeIfAbsent(groupBy, k -> search.getTimeSeries(k, "", search.from, search.to));
            series1.update(point[0], add(series1.get(point[0]) , point[1]));
        }));
        return new ArrayList<>(results.values());
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        return (currentValue, newValue, nextLine, position, time, expression) -> currentValue == -1 ? currentValue + 2 : currentValue + 1;
    }
}
