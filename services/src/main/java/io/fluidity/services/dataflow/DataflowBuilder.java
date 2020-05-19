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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.fluidity.dataflow.Model.CORR_HIST_PREFIX;

public class DataflowBuilder {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(DataflowBuilder.class);

    public DataflowBuilder() {
        log.info("Created");
    }

    public FileMeta[] listFiles(String tenant, Search search, QueryService query) {
        List<FileMeta> files = query.list(tenant).stream().filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime)).limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    public String extractCorrelationData(String session, FileMeta[] files, Search search, Storage storage, String region, String tenant, String modelPath) {
        Arrays.stream(files).forEach((item) -> extractCorrelationData(session, item, search, storage, region, tenant, modelPath));
        return "done";
    }

    private String extractCorrelationData(String session, FileMeta fileMeta, Search search, Storage storage, String region, String tenant, String modelPath) {
        try {
            log.info(LogHelper.format(session, "builder", "extractFlow", "File:" + fileMeta.filename));
            String fileUrl = fileMeta.getStorageUrl();
            InputStream inputStream = getInputStream(storage, region, tenant, fileUrl);

            String status = "";
            try (
                    DataflowExtractor dataflowExtractor = new DataflowExtractor(inputStream, getOutStreamFactory(storage), modelPath, region, tenant)
            ) {
                status = dataflowExtractor.process(fileMeta.isCompressed(), search, fileMeta.fromTime, fileMeta.toTime, fileMeta.size, fileMeta.timeFormat);
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

    private InputStream getInputStream(Storage storage, String region, String tenant, String fileUrl) throws IOException {
        InputStream inputStream = storage.getInputStream(region, tenant, fileUrl);
        if (fileUrl.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        if (fileUrl.endsWith(".lz4")) {
            inputStream = new LZ4FrameInputStream(inputStream);
        }
        return inputStream;
    }

    public String status(String session, String modelName) {
        return "dunno!";
    }

    public List<Map<String, String>> getModel(String region, String tenant, String session, String modelName, Storage storage) {
        List<Map<String, String>> histoUrls = new ArrayList<>();
        storage.listBucketAndProcess(region, tenant, modelName, (region1, itemUrl, itemName, modified) -> {
            if (itemUrl.contains(CORR_HIST_PREFIX)) {
                histoUrls.add(Map.of("name", itemUrl, "modified", Long.toString(modified)));
            }
            return null;
        });
        return histoUrls;
    }
}