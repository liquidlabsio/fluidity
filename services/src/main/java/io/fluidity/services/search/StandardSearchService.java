package io.fluidity.services.search;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.FileMetaDataQueryService;
import io.fluidity.services.storage.Storage;
import io.fluidity.search.Search;
import io.fluidity.search.agg.events.LineByLineEventAggregator;
import io.fluidity.search.agg.events.SearchEventCollector;
import io.fluidity.search.agg.histo.HistoAggFactory;
import io.fluidity.search.agg.histo.HistoAggregator;
import io.fluidity.search.agg.histo.SimpleHistoCollector;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class StandardSearchService implements SearchService {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(StandardSearchService.class);

    public StandardSearchService() {
        log.info("Created");
    }

    @Override
    public FileMeta[] submit(Search search, FileMetaDataQueryService query) {
        List<FileMeta> files = query.list().stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    @Override
    public String[] searchFile(FileMeta[] files, Search search, Storage storage, String region, String tenant) {
        try {
            FileMeta fileMeta = files[0];
            String searchUrl = fileMeta.getStorageUrl();
            InputStream inputStream = getInputStream(storage, region, tenant, searchUrl);

            String searchDestinationUrl = search.eventsDestinationURI(storage.getBucketName(tenant), searchUrl);
            OutputStream outputStream = storage.getOutputStream(region, tenant, searchDestinationUrl, 1);

            String histoDestinationUrl = search.histoDestinationURI(storage.getBucketName(tenant), searchUrl);
            OutputStream histoOutputStream = storage.getOutputStream(region, tenant, histoDestinationUrl, 1);

            try (
                    SearchEventCollector searchProcessor = new SearchEventCollector();
                    SimpleHistoCollector histoCollector = new SimpleHistoCollector(histoOutputStream, fileMeta.filename, fileMeta.tags, fileMeta.storageUrl, search, search.from, search.to, new HistoAggFactory().getHistoAnalyticFunction(search))
            ) {
                searchProcessor.process(fileMeta.isCompressed(), histoCollector, search, inputStream, outputStream, fileMeta.fromTime, fileMeta.toTime, fileMeta.size, fileMeta.timeFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new String[]{"histo", "search"};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private InputStream getInputStream(Storage storage, String region, String tenant, String searchUrl) throws IOException {
        InputStream inputStream = storage.getInputStream(region, tenant, searchUrl);
        if (searchUrl.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        if (searchUrl.endsWith(".lz4")) {
            inputStream = new LZ4FrameInputStream(inputStream);
        }
        return inputStream;
    }

    @Override
    public String finalizeHisto(Search search, String tenant, String region, Storage storage) {

        long start = System.currentTimeMillis();
        String histoAggJsonData = "";
        try (HistoAggregator histoAgg = new HistoAggFactory().get(storage.getInputStreams(region, tenant, search.stagingPrefix(), Search.histoSuffix, 0), search)) {
            histoAggJsonData = histoAgg.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("HistoElapsed:{}", (System.currentTimeMillis() - start));

        return histoAggJsonData;
    }

    @Override
    public String[] finalizeEvents(Search search, long fromTime, int limit, String tenant, String region, Storage storage) {

        long start = System.currentTimeMillis();

        String[] eventAggs;

        Map<String, InputStream> inputStreams = storage.getInputStreams(region, tenant, search.stagingPrefix(), Search.eventsSuffix, fromTime);
        try (LineByLineEventAggregator eventAggregator = new LineByLineEventAggregator(inputStreams, search)) {
            eventAggs = eventAggregator.process(fromTime, limit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("EventsElapsed:{}", (System.currentTimeMillis() - start));

        return eventAggs;
    }

}