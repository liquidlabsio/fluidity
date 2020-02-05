package io.precognito.services.search;

public interface SearchService {

    String[] submit(Search search);
    String[] searchFile(String[] files, Search search);
    String[] finalizeResults(String[] files, Search search);

}
