package io.fluidity.services.dataflow;

import io.fluidity.search.Search;

public interface DataflowService {
    String submit(String tenant, Search search, String modelname, String serviceAddress);

    String status(String tenant, String session, String modelName);

    String model(String tenant, String session, String modelName);

    String rewriteCorrelationData(String tenant, String session, String fileMetas, String modelPath, Search search);
}
