package io.fluidity.dataflow;

import io.fluidity.search.Search;

/**
 * Represents stages 2 and 3 from: https://github.com/liquidlabsio/fluidity/issues/50
 */
public class DataflowModeller {
    public DataflowModeller(String stagingLocation, String APIGatewayForFanOut) {

    }

    /**
     * Fan out to build individual correlation.index models
     */
    public void buildCorrelationIndexes() {

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
