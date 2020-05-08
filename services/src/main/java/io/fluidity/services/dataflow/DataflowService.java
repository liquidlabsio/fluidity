package io.fluidity.services.dataflow;

import io.fluidity.search.Search;

public interface DataflowService {
    String submit(String tenant, Search search, String serviceAddress);

    String status(String tenant, String session);

    String model(String tenant, String session);

    String rewriteCorrelationData(String tenant, String fileMetas, String modelPath, Search search);
}
