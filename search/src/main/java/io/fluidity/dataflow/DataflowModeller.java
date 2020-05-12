package io.fluidity.dataflow;

import io.fluidity.search.Search;
import org.graalvm.collections.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents stages 2 and 3 from: https://github.com/liquidlabsio/fluidity/issues/50
 */
public class DataflowModeller {
    public DataflowModeller() {

    }

    public FlowInfo getCorrelationFlow(String flowId, List<Pair<Long, String>> correlationSet) {
        List<Long[]> durations = getDurations(correlationSet);
        return new FlowInfo(flowId, getFlowFiles(correlationSet), durations);
    }

    private List<Long[]> getDurations(List<Pair<Long, String>> correlationSet) {
        List<Long[]> longs = correlationSet.stream().map(pair -> {
            String filename = pair.getRight();
            String[] split = filename.split(Model.DELIM);
            return new Long[]{Long.parseLong(split[2]), Long.parseLong(split[3])};
        }).collect(Collectors.toList());
        return longs;
    }

    private List<String> getFlowFiles(List<Pair<Long, String>> correlationSet) {
        return correlationSet.stream().map(pair -> pair.getRight()).collect(Collectors.toList());
    }

    /**
     * Aggregqate all correlation.indexes to a timeseries histogram and store in the cloud
     *
     * @param search
     * @return the histogram of the stored model
     */
    public String buildModelFromIndexes(Search search) {
        return null;
    }
}
