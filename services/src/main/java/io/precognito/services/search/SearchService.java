package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;

public interface SearchService {

    FileMeta[] submit(Search search, FileMetaDataQueryService query);

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

    /**
     * Returns [ numEvents, Histo, rawEvents ]
     *
     * @param histos
     * @param events
     * @param search
     * @param tenant
     * @param region
     * @param storage
     * @return
     */
    String[] finalizeResults(String histos, String events, Search search, String tenant, String region, Storage storage);
}
