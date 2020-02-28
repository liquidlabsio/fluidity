package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.search.agg.CountingHistoAggregator;
import io.precognito.search.agg.HistoAggregator;
import io.precognito.search.agg.SeriesHistoAggregator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoAggFactory {

    List<HistoAggregator> aggs = Arrays.asList(new CountingHistoAggregator(null, null), new SeriesHistoAggregator(null, null));


    public HistoAggregator get(Map<String, InputStream> inputStreams, Search search) {
        String analytic = search.analyticValue();
        List<HistoAggregator> histoAggregators = aggs.stream().filter(item -> item.isForMe(analytic)).collect(Collectors.toList());
        if (histoAggregators.isEmpty()) {
            return new CountingHistoAggregator(inputStreams, search);
        } else {
            return histoAggregators.iterator().next().clone(inputStreams, search);
        }
    }
}
