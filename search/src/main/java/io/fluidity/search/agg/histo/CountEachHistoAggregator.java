package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class CountEachHistoAggregator extends AbstractHistoAggregator {

    public boolean isForMe(String analytic) {
        return analytic.equals("analytic.countEach()");
    }

    public static final int LIMIT = 25;

    public CountEachHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        super(inputStreams, search);
    }

    @Override
    List<Series> processSeries(Collection<Series> collectedSeries) {
        final Set<String> topSeries = getTopSeriesNames(collectedSeries, LIMIT);

        Map<String, Series> results = new HashMap<>();

        // collect top items
        collectedSeries.stream().filter(series -> topSeries.contains(series.name())).forEach(series -> {
            if (results.containsKey(series.name())) {
                Series series1 = results.get(series.name());
                series1.merge(series);
            } else {
                results.put(series.name(), series);
            }
        });

        // collect other items
        Series other = search.getTimeSeries("other", search.from, search.to);
        results.put(other.name(), other);

        collectedSeries.stream().filter(series -> !topSeries.contains(series.name())).forEach(series -> other.merge(series));
        return new ArrayList<>(results.values());
    }

    private Set<String> getTopSeriesNames(Collection<Series> collectedSeries, int limit) {
        // count the total hits for the series
        Map<String, Long> countMap = new HashMap<>();
        collectedSeries.stream().forEach(series -> series.data().stream().forEach(data -> {
            Long aLong = countMap.get(series.name());
            if (aLong == null || aLong == -1) aLong = 0L;
            countMap.put(series.name(), add(aLong, data[1]));
        }));
        // sort by number of hits
        List<Map.Entry<String, Long>> entryArrayList = new ArrayList<>(countMap.entrySet());
        Collections.sort(entryArrayList, Comparator.comparingLong(Map.Entry::getValue));

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
    public HistoFunction function() {
        return (currentValue, newValue, nextLine, position, time, expression) ->  currentValue == -1? currentValue + 2 : currentValue + 1;
    }
}
