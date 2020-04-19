package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoAggFactory {

    List<HistoAggregator> aggs = Arrays.asList(
            new CountHistoAggregator(null, null),
            new CountEachHistoAggregator(null, null),
            new CountDistinctHistoAggregator(null, null),
            new StatsHistoAggregator(null, null));

    public HistoAggregator get(Map<String, InputStream> inputStreams, Search search) {
        String analytic = search.analyticValue();
        List<HistoAggregator> histoAggregators = aggs.stream().filter(item -> item.isForMe(analytic)).collect(Collectors.toList());
        if (histoAggregators.isEmpty()) {
            return new CountHistoAggregator(inputStreams, search);
        } else {
            return histoAggregators.iterator().next().clone(inputStreams, search);
        }
    }


    public static HistoFunction Count = (currentValue, newValue, nextLine, position, time, expression) -> currentValue == -1 ? currentValue + 2 : currentValue +1;

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
