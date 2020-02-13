package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.search.agg.HistoAggregator;
import io.precognito.search.agg.SimpleHistoAggregator;
import io.precognito.search.agg.SimpleLineByLineAggregator;
import io.precognito.search.agg.SimpleHistoCollector;
import io.precognito.search.processor.SimpleSearchProcessor;
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

        String histoDestination = search.getHistoDestination(storage.getBucketName(tenant), searchUrl);
        OutputStream histoOutputStream = storage.getOutputStream(region, tenant, histoDestination);

        try (
                SimpleSearchProcessor searchProcessor = new SimpleSearchProcessor();
                SimpleHistoCollector histoCollector = new SimpleHistoCollector(histoOutputStream, fileMeta.filename, fileMeta.tags, fileMeta.storageUrl, search, search.from, search.to);
        ) {
            searchProcessor.process(histoCollector, search, inputStream, outputStream, fileMeta.fromTime, fileMeta.toTime, fileMeta.size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[] { histoDestination, searchDestination };
    }

    @Override
    public String[] finalizeResults(List<String> histoSourceUrls, List<String> eventSourceUrls, Search search, String tenant, String region, Storage storage) {
        String histoAggJsonData = "";
        try (HistoAggregator histoAgg = new SimpleHistoAggregator(storage.getInputStreams(region, tenant, histoSourceUrls), search)) {
            histoAggJsonData = histoAgg.process();

        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] eventAggs = new String[] { "", "" };

        try (SimpleLineByLineAggregator eventAggregator = new SimpleLineByLineAggregator(storage.getInputStreams(region, tenant, eventSourceUrls), search)){
            eventAggs = eventAggregator.process();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  new String[] { histoAggJsonData, eventAggs[0], eventAggs[1]};
    }
}