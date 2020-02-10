package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;

import java.util.List;

public interface SearchService {
    FileMeta[] submit(Search search, FileMetaDataQueryService query);

    /**
     *
     * @param files - set of files to search
     * @param lastMods - last modified times for each file
     * @param search
     * @param storage
     * @param region
     * @param tenant
     * @return
     */
    String[] searchFile(String[] files, Long[] lastMods, Search search, Storage storage, String region, String tenant);

    /**
     * Returns [ numEvents, Histo, rawEvents ]
     * @param histos
     * @param events
     * @param search
     * @param tenant
     * @param region
     * @param storage
     * @return
     */
    String[] finalizeResults(List<String> histos, List<String> events, Search search, String tenant, String region, Storage storage);
}
