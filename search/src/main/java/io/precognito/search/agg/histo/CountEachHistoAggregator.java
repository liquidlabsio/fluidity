package io.precognito.search.agg.histo;

import io.precognito.search.Search;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class CountEachHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.countEach()");
    }

    public static final int LIMIT = 10;

    public CountEachHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    @Override
    List<Series> processSeries(List<Series> collectedSeries) {
        final List<String> topSeries = getTopSeriesNames(collectedSeries, LIMIT);

        List<Series> results = new ArrayList<>();
        Series other = new TimeSeries("other", search.from, search.to);

        // collect top items
        collectedSeries.stream().filter(series -> topSeries.contains(series.name())).forEach(series -> results.add(series));

        // collect other items
        results.add(other);

        collectedSeries.stream().filter(series -> !topSeries.contains(series.name()))
                .forEach(series -> series.data().stream()
                        .forEach(dataPoint ->
                                other.update(dataPoint[0], other.get(dataPoint[0]) + dataPoint[1])
                        ));
        return results;
    }

    private List<String> getTopSeriesNames(List<Series> collectedSeries, int limit) {
        // count the total hits for the series
        Map<String, Long> countMap = new HashMap<>();
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(data -> {
            Long aLong = countMap.get(series.name());
            if (aLong == null) aLong = 0L;
            countMap.put(series.name(), aLong + data[1]);
        }));
        // sort by number of hits
        List<Map.Entry<String, Long>> entryArrayList = new ArrayList<>(countMap.entrySet());
        Collections.sort(entryArrayList, Comparator.comparingLong(Map.Entry::getValue));

        if (entryArrayList.size() > limit) {
            entryArrayList = entryArrayList.subList(0, limit);
        }

        // strip out the names
        return entryArrayList.stream().map(item -> item.getKey()).collect(Collectors.toList());
    }

    @Override
    public HistoAggregator clone(Map<String, InputStream> inputStreams, Search search) {
        return new CountEachHistoAggregator(inputStreams, search);
    }

    @Override
    public HistoFunction function() {
        return (currentValue, newValue, nextLine, position, time, expression) -> currentValue + 1;
    }
}
