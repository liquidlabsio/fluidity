package io.precognito.services.search;

import io.precognito.search.Search;
import io.precognito.search.agg.HistoAggregator;
import io.precognito.search.agg.SimpleHistoCollector;
import io.precognito.search.agg.SimpleLineByLineAggregator;
import io.precognito.search.processor.SimpleSearchProcessor;
import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import io.precognito.services.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class FixturedSearchService implements SearchService {
    private final Logger log = LoggerFactory.getLogger(FixturedSearchService.class);

    public FixturedSearchService() {
        log.info("Created");
    }

    @Override
    public FileMeta[] submit(Search search, FileMetaDataQueryService query) {
        List<FileMeta> files = query.list().stream().filter(file -> search.fileMatches(file.filename, file.fromTime, file.toTime)).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    @Override
    public String[] searchFile(FileMeta[] files, Search search, Storage storage, String region, String tenant) {
        try {
            FileMeta fileMeta = files[0];
            String searchUrl = fileMeta.getStorageUrl();
            InputStream inputStream = storage.getInputStream(region, tenant, searchUrl);
            String searchDestinationUrl = search.getEventsDestinationURI(storage.getBucketName(tenant), searchUrl);
            OutputStream outputStream = storage.getOutputStream(region, tenant, searchDestinationUrl);

            String histoDestinationUrl = search.getHistoDestinationURI(storage.getBucketName(tenant), searchUrl);
            OutputStream histoOutputStream = storage.getOutputStream(region, tenant, histoDestinationUrl);

            try (
                    SimpleSearchProcessor searchProcessor = new SimpleSearchProcessor();
                    SimpleHistoCollector histoCollector = new SimpleHistoCollector(histoOutputStream, fileMeta.filename, fileMeta.tags, fileMeta.storageUrl, search, search.from, search.to)
            ) {
                searchProcessor.process(histoCollector, search, inputStream, outputStream, fileMeta.fromTime, fileMeta.toTime, fileMeta.size);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new String[]{histoDestinationUrl, searchDestinationUrl};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    @Override
    public String[] finalizeResults(String histoSourceUrls, String eventSourceUrls, Search search, String tenant, String region, Storage storage) {
        String histoAggJsonData = "";
        try (HistoAggregator histoAgg = new HistoAggFactory().get(storage.getInputStreams(region, tenant, search.getStagingPrefix(), Search.histoSuffix), search)) {
            histoAggJsonData = histoAgg.process();

        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] eventAggs = new String[]{"", ""};

        try (SimpleLineByLineAggregator eventAggregator = new SimpleLineByLineAggregator(storage.getInputStreams(region, tenant, search.getStagingPrefix(), Search.eventsSuffix), search)) {
            eventAggs = eventAggregator.process();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  new String[] { histoAggJsonData, eventAggs[0], eventAggs[1]};
    }
}