/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.dataflow;

import io.fluidity.dataflow.DataflowExtractor;
import io.fluidity.dataflow.FlowLogHelper;
import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.fluidity.dataflow.Model.CORR_HIST_PREFIX;
import static io.fluidity.dataflow.Model.LADDER_HIST_PREFIX;

public class DataflowBuilder {
    private long limitList = 5000;
    private final Logger log = LoggerFactory.getLogger(DataflowBuilder.class);

    public DataflowBuilder() {
        log.info("Created");
    }

    public FileMeta[] listFiles(final String tenant, final Search search, final QueryService query) {
        final List<FileMeta> files = query.list(tenant).stream()
                .filter(file -> search.tagMatches(file.getTags()) && search.fileMatches(file.filename, file.fromTime, file.toTime))
                .limit(limitList).collect(Collectors.toList());
        return files.toArray(new FileMeta[0]);
    }

    public String extractCorrelationData(final String session, final FileMeta[] files, final Search search,
                                         final Storage storage, final String region, final String tenant,
                                         final String modelPath) {
        Arrays.stream(files).forEach((item) -> extractCorrelationData(session, item, search, storage, region, tenant, modelPath));
        return "done";
    }

    private String extractCorrelationData(final String session, final FileMeta fileMeta, final Search search,
                                          final Storage storage, final String region, final String tenant,
                                          final String modelPath) {
        try {
            log.info(FlowLogHelper.format(session, "builder", "extractFlow", "File:" + fileMeta.filename));
            final String fileUrl = fileMeta.getStorageUrl();
            final StorageInputStream inputStream = getInputStream(storage, region, tenant, fileUrl);

            String status = "";
            try (
                    DataflowExtractor dataflowExtractor = new DataflowExtractor(inputStream, getOutStreamFactory(storage),
                            modelPath, region, tenant)
            ) {
                status = dataflowExtractor.process(fileMeta.isCompressed(), search, fileMeta.fromTime, fileMeta.toTime,
                        fileMeta.size, fileMeta.timeFormat);
            } catch (Exception e) {
                log.warn("Failed to process:" + fileMeta.filename, e);
                e.printStackTrace();
            }
            return status;
        } catch (Exception e) {
            log.info(FlowLogHelper.format(session, "builder", "extractFlow", "Error:" + e.toString()));
            log.warn("Failed to process:", e);
            return e.toString();
        } finally {
            log.info(FlowLogHelper.format(session, "builder", "extractFlow", "Finished"));
        }
    }

    private StorageUtil getOutStreamFactory(final Storage storage) {
        final StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            try (OutputStream storageOutputStream = storage.getOutputStream(region, tenant, filePath, daysRetention, lastModified)) {
                // duplicate copy to local FS due to not knowing the name until finished
                IOUtils.copy(inputStream, storageOutputStream);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return outFactory;
    }

    private StorageInputStream getInputStream(final Storage storage, final String region, final String tenant,
                                              final String fileUrl) throws IOException {
        StorageInputStream inputStream = storage.getInputStream(region, tenant, fileUrl);
        if (fileUrl.endsWith(".gz")) {
            inputStream = inputStream.copy(new GZIPInputStream(inputStream.inputStream));
        }
        if (fileUrl.endsWith(".lz4")) {
            inputStream = inputStream.copy(new LZ4FrameInputStream(inputStream.inputStream));
        }
        return inputStream;
    }

    public String status(final String session, final String modelName) {
        return "dunno!";
    }

    public List<Map<String, String>> getModelDataList(final String region, final String tenant, final String session,
                                                      final String modelName, final Storage storage) {
        final List<Map<String, String>> ladderAndHistoUrls = new ArrayList<>();
        storage.listBucketAndProcess(region, tenant, modelName, (region1, itemUrl, itemName, modified, size) -> {
            if (itemUrl.contains(CORR_HIST_PREFIX) || itemUrl.contains(LADDER_HIST_PREFIX) ) {
                ladderAndHistoUrls.add(Map.of("name", itemUrl, "modified", Long.toString(modified), "size", Long.toString(size)));
            }
        });
        return ladderAndHistoUrls;
    }
}
