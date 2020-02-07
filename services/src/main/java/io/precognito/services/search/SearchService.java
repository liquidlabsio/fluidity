package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;

public interface SearchService {
    String[] submit(Search search, FileMetaDataQueryService query);
    String[] searchFile(String[] files, Search search, Storage storage, String region, String tenant);
    String[] finalizeResults(String[] files, Search search);
}
