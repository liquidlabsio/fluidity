package io.fluidity.services.search;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.FileMetaDataQueryService;
import io.fluidity.services.storage.Storage;
import io.fluidity.search.Search;

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
