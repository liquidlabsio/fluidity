package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.search.agg.SimpleRawFileAggregator;
import io.precognito.search.processor.SimpleSearch;
import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class FixturedSearchService implements SearchService {
    private final Logger log = LoggerFactory.getLogger(FixturedSearchService.class);

    public FixturedSearchService() {
        log.info("Created");
    }

    @Override
    public FileMeta[] submit(Search search, FileMetaDataQueryService query) {
        // TODO: filter filenames etc
        return query.list().toArray(new FileMeta[0]);
    }

    @Override
    public String[] searchFile(FileMeta[] files, Search search, Storage storage, String region, String tenant) {
        FileMeta fileMeta = files[0];
        String searchUrl = fileMeta.getStorageUrl();
        InputStream inputStream = storage.getInputStream(region, tenant, searchUrl);
        String searchDestination = search.getSearchDestination(storage.getBucketName(tenant), searchUrl);
        OutputStream outputStream = storage.getOutputStream(region, tenant, searchDestination);

        try (SimpleSearch searchProcessor = new SimpleSearch()) {
            searchProcessor.process(search, inputStream, outputStream, fileMeta.fromTime, fileMeta.toTime, fileMeta.size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[] { "s3://no-histo-available", searchDestination };
    }

    @Override
    public String[] finalizeResults(List<String> histos, List<String> events, Search search, String tenant, String region, Storage storage) {
        try (SimpleRawFileAggregator aggregator = new SimpleRawFileAggregator(storage.getInputStreams(region, tenant, events), search)){
            return aggregator.process();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}