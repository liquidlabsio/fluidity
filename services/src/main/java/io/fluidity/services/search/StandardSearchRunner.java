/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.search.agg.events.EventCollector;
import io.fluidity.search.agg.events.LineByLineEventAggregator;
import io.fluidity.search.agg.events.SearchEventCollector;
import io.fluidity.search.agg.histo.HistoAggFactory;
import io.fluidity.search.agg.histo.HistoAggregator;
import io.fluidity.search.agg.histo.HistoCollector;
import io.fluidity.search.agg.histo.SimpleHistoCollector;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
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

public class StandardSearchRunner implements SearchRunner {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(StandardSearchRunner.class);

    public StandardSearchRunner() {
        log.info("Created");
    }

    @Override
    public FileMeta[] submit(Search search, QueryService query) {
        List<FileMeta> files = query.list().stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    @Override
    public String[] searchFile(FileMeta fileMeta, Search search, Storage storage, String region, String tenant) {
        try {
            String searchUrl = fileMeta.getStorageUrl();
            InputStream inputStream = getInputStream(storage, region, tenant, searchUrl);


            int[] processedEventsAndTotal = new int[]{0, 0};
            try (
                    EventCollector searchProcessor = getCollectors(search, storage, tenant, searchUrl, inputStream, fileMeta, region)
            ) {
                processedEventsAndTotal = searchProcessor.process(fileMeta.isCompressed(), search, fileMeta.fromTime, fileMeta.toTime, fileMeta.size, fileMeta.timeFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new String[]{"histo", Integer.toString(processedEventsAndTotal[0]), Integer.toString(processedEventsAndTotal[1])};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private EventCollector getCollectors(Search search, Storage storage, String tenant, String searchUrl, InputStream inputStream, FileMeta fileMeta, String region) {
        String searchDestinationUrl = search.eventsDestinationURI(storage.getBucketName(tenant), searchUrl);
        OutputStream outputStream = storage.getOutputStream(region, tenant, searchDestinationUrl, 1);

        String histoDestinationUrl = search.histoDestinationURI(storage.getBucketName(tenant), searchUrl);
        OutputStream histoOutputStream = storage.getOutputStream(region, tenant, histoDestinationUrl, 1);

        HistoCollector histoCollector = new SimpleHistoCollector(histoOutputStream, fileMeta.filename, fileMeta.tags, search, search.from, search.to, new HistoAggFactory().getHistoAnalyticFunction(search));
        return new SearchEventCollector(histoCollector, inputStream, outputStream);
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