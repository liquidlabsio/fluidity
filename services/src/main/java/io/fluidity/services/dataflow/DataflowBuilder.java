package io.fluidity.services.dataflow;

import io.fluidity.dataflow.DataflowExtractor;
import io.fluidity.dataflow.LogHelper;
import io.fluidity.search.Search;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.fluidity.dataflow.DataflowExtractor.CORR_HIST_PREFIX;

public class DataflowBuilder {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(DataflowBuilder.class);

    public DataflowBuilder() {
        log.info("Created");
    }

    public FileMeta[] listFiles(Search search, QueryService query) {
        List<FileMeta> files = query.list().stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    public String extractCorrelationData(String session, FileMeta[] files, Search search, Storage storage, String region, String tenant, String filePrefix) {
        try {
            FileMeta fileMeta = files[0];
            log.info(LogHelper.format(session, "builder", "extractFlow", "File:" + fileMeta.filename));
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
            log.info(LogHelper.format(session, "builder", "extractFlow", "Error:" + e.toString()));
            log.warn("Failed to process:", e);
            return e.toString();
        } finally {
            log.info(LogHelper.format(session, "builder", "extractFlow", "Finished"));
        }
    }

    private StorageUtil getOutStreamFactory(Storage storage) {
        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            try (OutputStream storageOutputStream = storage.getOutputStream(region, tenant, filePath, daysRetention)) {
                // duplicate copy to local FS due to not knowing the name until finished
                IOUtils.copy(inputStream, storageOutputStream);
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

    public String status(String session, String modelName) {
        return "dunno!";
    }

    public String getModel(String region, String tenant, String session, String modelName, Storage storage) {
        List<String> histoList = new ArrayList<>();
        storage.listBucketAndProcess(region, tenant, modelName + "/" + CORR_HIST_PREFIX, (region1, itemUrl, itemName) -> {
            histoList.add(itemUrl);
            return null;
        });

        StringBuilder results = new StringBuilder();
        histoList.stream().forEach(histoFileurl -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream inputStream = storage.getInputStream(region, tenant, histoFileurl)) {
                IOUtils.copy(inputStream, baos);
                results.append(baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return results.toString();
    }
}