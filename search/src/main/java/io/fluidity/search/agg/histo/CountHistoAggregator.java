package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.*;

public class CountHistoAggregator extends AbstractHistoAggregator<Long> {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.count()");
    }

    public CountHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
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
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction<Long, Long> function() {
        return (currentValue, newValue, nextLine, position, time, histoIndex, expression) -> currentValue == null ? 1l : currentValue.longValue() + 1l;
    }
}
