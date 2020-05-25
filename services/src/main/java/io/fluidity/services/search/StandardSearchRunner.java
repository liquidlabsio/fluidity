/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;
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
import java.util.ArrayList;
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
    public FileMeta[] submit(String tenant, Search search, QueryService query) {
        List<FileMeta> files = query.list(tenant).stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    @Override
    public List<Integer[]> searchFile(FileMeta[] fileMetaBatch, Search search, Storage storage, String region, String tenant) {


        /**
         * Use a single histo aggregator to the firstFile output stream - it can aggregate from others in the same batch - reducing the total number of files for later aggregation
         */
        FileMeta firstFile = fileMetaBatch[0];
        String histoDestinationUrl = search.histoDestinationURI(storage.getBucketName(tenant), firstFile.getStorageUrl());
        OutputStream histoOutputStream = storage.getOutputStream(region, tenant, histoDestinationUrl, 1);

        List<Integer[]> results = new ArrayList<>();
        try (HistoCollector histoCollector = new SimpleHistoCollector(histoOutputStream, search, search.from, search.to, new HistoAggFactory().getHistoAnalyticFunction(search))) {

            for (FileMeta fileMeta : fileMetaBatch) {

                String searchUrl = fileMeta.getStorageUrl();
                StorageInputStream inputStream = getInputStream(storage, region, tenant, searchUrl);
                histoCollector.updateFileInfo(fileMeta.filename, firstFile.tags);

                try (
                        EventCollector searchProcessor = getCollectors(search, storage, tenant, searchUrl, inputStream.inputStream, region, histoCollector)
                ) {
                    results.add(searchProcessor.process(fileMeta.isCompressed(), search, fileMeta.fromTime, inputStream.lastModified, inputStream.length, fileMeta.timeFormat));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private EventCollector getCollectors(Search search, Storage storage, String tenant, String searchUrl, InputStream inputStream, String region, HistoCollector histoCollector) {
        String searchDestinationUrl = search.eventsDestinationURI(storage.getBucketName(tenant), searchUrl);
        OutputStream outputStream = storage.getOutputStream(region, tenant, searchDestinationUrl, 1);
        return new SearchEventCollector(histoCollector, inputStream, outputStream);
    }

    private StorageInputStream getInputStream(Storage storage, String region, String tenant, String searchUrl) throws IOException {
        StorageInputStream inputStream = storage.getInputStream(region, tenant, searchUrl);
        if (searchUrl.endsWith(".gz")) {
            inputStream = inputStream.copy(new GZIPInputStream(inputStream.inputStream));
        }
        if (searchUrl.endsWith(".lz4")) {
            inputStream = inputStream.copy(new LZ4FrameInputStream(inputStream.inputStream));
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

        Map<String, StorageInputStream> inputStreams = storage.getInputStreams(region, tenant, search.stagingPrefix(), Search.eventsSuffix, fromTime);
        try (LineByLineEventAggregator eventAggregator = new LineByLineEventAggregator(inputStreams, search)) {
            eventAggs = eventAggregator.process(fromTime, limit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("EventsElapsed:{}", (System.currentTimeMillis() - start));

        return eventAggs;
    }
}