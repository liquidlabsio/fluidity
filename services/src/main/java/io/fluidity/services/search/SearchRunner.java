package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;

public interface SearchRunner {


    FileMeta[] submit(Search search, QueryService query);

    /**
     *
     * @param files - set of files to search
     * @param search
     * @param storage
     * @param region
     * @param tenant
     * @return
     */
    String[] searchFile(FileMeta[] files, Search search, Storage storage, String region, String tenant);

    String finalizeHisto(Search search, String tenant, String region, Storage storage);

    /**
     * Returns [ numEvents, Histo, rawEvents ]
     *
     * @param search
     * @param tenant
     * @param region
     * @param storage
     * @return
     */
    String[] finalizeEvents(Search search, long from, int limit, String tenant, String region, Storage storage);
}
