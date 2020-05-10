package io.fluidity.services.dataflow;

import io.fluidity.dataflow.DataflowExtractor;
import io.fluidity.dataflow.DataflowModeller;
import io.fluidity.search.Search;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.search.agg.histo.HistoAggFactory;
import io.fluidity.search.agg.histo.HistoAggregator;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class DataflowBuilder {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(DataflowBuilder.class);

    public DataflowBuilder() {
        log.info("Created");
    }

    public FileMeta[] submit(Search search, QueryService query) {
        List<FileMeta> files = query.list().stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    public String extractCorrelationData(FileMeta[] files, Search search, Storage storage, String region, String tenant, String filePrefix) {
        try {
            FileMeta fileMeta = files[0];
            String searchUrl = fileMeta.getStorageUrl();
            InputStream inputStream = getInputStream(storage, region, tenant, searchUrl);


            String status = "";
            try (
                    DataflowExtractor searchProcessor = new DataflowExtractor(inputStream, getOutStreamFactory(storage), filePrefix, region, tenant)
            ) {
                status = searchProcessor.process(fileMeta.isCompressed(), search, fileMeta.fromTime, fileMeta.toTime, fileMeta.size, fileMeta.timeFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return status;
        } catch (Exception e) {
            log.warn("Failed to process:", e);
            return e.toString();
        }
    }

    private StorageUtil getOutStreamFactory(Storage storage) {
        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            try {
                // duplicate copy to local FS due to not knowing the name until finished
                OutputStream storageOutputStream = storage.getOutputStream(region, tenant, filePath, daysRetention);
                IOUtils.copy(inputStream, storageOutputStream);
                storageOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return outFactory;
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

    public String finalizeHisto(Search search, String tenant, String region, Storage storage) {

        long start = System.currentTimeMillis();
        String histoAggJsonData = "";
        if (search.isNormalSearch()) {

            try (HistoAggregator histoAgg = new HistoAggFactory().get(storage.getInputStreams(region, tenant, search.stagingPrefix(), Search.histoSuffix, 0), search)) {
                histoAggJsonData = histoAgg.process();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // dataflow model building
            // we have a dataflow model-dir or stage-1 data ({ts}-correlation.log data
            // stage 2 we run-lambdas to build the indexes
            DataflowModeller dataflowModeller = new DataflowModeller("staging-dir", "http://gateway-api/");

            dataflowModeller.buildCorrelationIndexes();
            return dataflowModeller.buildModelFromIndexes(search);
        }
        log.info("HistoElapsed:{}", (System.currentTimeMillis() - start));

        return histoAggJsonData;
    }

    public String status(String session) {
        return null;
    }

    public String getModel(String session) {
        return "";
    }
}