package io.precognito.search.agg;

import io.precognito.search.Search;
import io.precognito.search.agg.histo.AvgHistoAggregator;
import io.precognito.search.agg.histo.CountingHistoAggregator;
import io.precognito.search.agg.histo.HistoAggregator;
import io.precognito.search.agg.histo.SeriesHistoAggregator;
import io.precognito.search.processor.HistoFunction;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoAggFactory {

    List<HistoAggregator> aggs = Arrays.asList(new CountingHistoAggregator(null, null), new SeriesHistoAggregator(null, null), new AvgHistoAggregator(null, null));

    public HistoAggregator get(Map<String, InputStream> inputStreams, Search search) {
        String analytic = search.analyticValue();
        List<HistoAggregator> histoAggregators = aggs.stream().filter(item -> item.isForMe(analytic)).collect(Collectors.toList());
        if (histoAggregators.isEmpty()) {
            return new CountingHistoAggregator(inputStreams, search);
        } else {
            return histoAggregators.iterator().next().clone(inputStreams, search);
        }
    }


    public static HistoFunction Count = (currentValue, newValue, nextLine, position, time, expression) -> currentValue + 1;

    public HistoFunction getHistoAnalyticFunction(Search search) {
        String analytic = search.analyticValue();
        List<HistoAggregator> histoAggregators = aggs.stream().filter(item -> item.isForMe(analytic)).collect(Collectors.toList());
        if (histoAggregators.isEmpty()) {
            return Count;
        } else {
            return histoAggregators.iterator().next().function();
        }
    }

}
