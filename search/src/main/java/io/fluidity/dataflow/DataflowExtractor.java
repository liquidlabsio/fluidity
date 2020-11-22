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

package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.search.field.extractor.KvJsonPairExtractor;
import io.fluidity.util.DateTimeExtractor;
import io.fluidity.util.DateUtil;
import org.graalvm.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fluidity.dataflow.Model.CORR_DAT_FMT;
import static io.fluidity.dataflow.Model.CORR_FILE_FMT;

/**
 * Extract correlated data to relevant files and stores within the CloudStorage Dataflow model directory.
 */
public class DataflowExtractor implements AutoCloseable {
    private final Logger log = LoggerFactory.getLogger(DataflowExtractor.class);

    private final StorageInputStream input;
    private final StorageUtil storageUtil;
    private String modelPath;
    private final String region;
    private final String tenant;
    private int logWarningCount = 0;

    public DataflowExtractor(StorageInputStream inputStream, StorageUtil storageUtil, String modelPath, String region, String tenant) {
        this.input = inputStream;
        this.storageUtil = storageUtil;
        this.modelPath = modelPath;
        this.region = region;
        this.tenant = tenant;
    }

    public String process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long fileLength, String timeFormat) throws IOException {

        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor(timeFormat);

        LinkedList<Integer> lengths = new LinkedList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(input.inputStream)));
        Optional<String> nextLine = Optional.ofNullable(reader.readLine());

        if (nextLine.isPresent()) lengths.add(nextLine.get().length());
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine);

        long scanFilePos = 0;
        File currentFile = null;
        Optional<OutputStream> bos = Optional.empty();
        String currentCorrelation = "nada";
        long startTime = 0;
        long lastCorrelationTime = 0;

        Map<String, KvJsonPairExtractor> extractorMap = getExtractorMap();
        Map<String, String> datData = new HashMap<>();
        AtomicInteger ops = new AtomicInteger();
        try {
            while (nextLine.isPresent()) {

                // recalibrate the time interval as more line lengths are known
                lengths.add(nextLine.get().length());
                guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, currentTime, fileToTime, fileLength, scanFilePos, lengths);
                scanFilePos += nextLine.get().length() + 2;

                if (search.matches(nextLine.get())) {
                    currentTime = dateTimeExtractor.getTimeMaybe(currentTime, guessTimeInterval, nextLine);

                    Optional<Pair<String, Long>> fieldNameAndValue = Optional.ofNullable(search.getFieldNameAndValue("file-name-source", nextLine.get()));

                    if (fieldNameAndValue.isPresent()) {
                        String correlationId = fieldNameAndValue.get().getLeft();
                        if (!currentCorrelation.equals(correlationId)) {
                            flushToStorage(bos, lastCorrelationTime, currentFile, startTime, datData, currentCorrelation, ops);
                            currentCorrelation = correlationId;
                            currentFile = File.createTempFile(correlationId, ".log");
                            bos = Optional.of(new BufferedOutputStream(new FileOutputStream(currentFile)));
                            bos.get().write(("source:" + input.name + " offset:" + scanFilePos + "\n").getBytes());
                            startTime = currentTime;
                            ops.set(0);
                        }
                        getDatData(ops, nextLine.get(), datData, extractorMap);
                        if (bos.isPresent()) {
                            bos.get().write(nextLine.get().getBytes());
                            bos.get().write('\n');
                        }
                    }
                    lastCorrelationTime = currentTime;
                }
                nextLine = Optional.ofNullable(reader.readLine());
            }
        } finally {
            reader.close();
            flushToStorage(bos, lastCorrelationTime, currentFile, startTime, datData, currentCorrelation, ops);
        }
        return "done";
    }

    private void flushToStorage(Optional<OutputStream> bos, long lastCorrelationTime, File currentFile, long startTime, Map<String, String> datData, String correlationId, AtomicInteger ops) throws IOException {
        if (bos.isPresent()) {
            bos.get().close();
            // in case time extraction is being auto-calculated and got it wrong.
            // better to use exact timestamp matching
            if (lastCorrelationTime < startTime) {
                lastCorrelationTime = startTime+1000;
            }
            storageUtil.copyToStorage(new FileInputStream(currentFile), region, tenant, String.format(CORR_FILE_FMT, modelPath, startTime, lastCorrelationTime, correlationId), 365, startTime);
            currentFile.delete();
            datData.put("operations", ops.toString());
            storageUtil.copyToStorage(makeDatFile(datData), region, tenant, String.format(CORR_DAT_FMT, modelPath, startTime, lastCorrelationTime, correlationId), 365, startTime);
            datData.clear();
        }
    }

    private InputStream makeDatFile(Map<String, String> datData) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;
        try {
            json = objectMapper.writeValueAsString(datData);
        } catch (JsonProcessingException e) {
            json = e.toString();

        }
        return new ByteArrayInputStream(json.getBytes());
    }

    private Map<String, KvJsonPairExtractor> getExtractorMap() {
        // look for json information about which stage of a trace or the name of the service being processed
        HashMap<String, KvJsonPairExtractor> extractorMap = new HashMap<>();
        // loginService
        addToMap(extractorMap, new KvJsonPairExtractor("service"));
        // doStuff
        addToMap(extractorMap, new KvJsonPairExtractor("operation"));
        // REST, SQL, Lambda, Micro-thingy
        addToMap(extractorMap, new KvJsonPairExtractor("type"));
        // anthing else that is useful
        addToMap(extractorMap, new KvJsonPairExtractor("meta"));
        // tag information
        addToMap(extractorMap, new KvJsonPairExtractor("tag"));
        // normal/error/warn information
        addToMap(extractorMap, new KvJsonPairExtractor("behavior"));
        return extractorMap;
    }

    private void addToMap(HashMap<String, KvJsonPairExtractor> extractorMap, KvJsonPairExtractor extractor) {
        extractorMap.put(extractor.getToken(), extractor);
    }

    private void getDatData(AtomicInteger ops, String nextLine, Map<String, String> datData, Map<String, KvJsonPairExtractor> extractorMap) {
        extractorMap.values().stream().forEach(extractor -> {
            try {
                Optional<Pair<String, Long>> extracted = Optional.ofNullable(extractor.getKeyAndValue("none", nextLine));
                if (extracted.isPresent()) {
                    String currentValue = datData.get(extractor.getToken());
                    if (currentValue == null) currentValue = "";
                    else currentValue = currentValue + ", ";
                    datData.put(extractor.getToken(), currentValue + extracted.get().getLeft());
                    if (extractor.getToken().equals("operation")) {
                        ops.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                if (logWarningCount++ < 10) {
                    log.warn("Extractor Failed:" + extractor.getToken(), e);
                }
            }
        });
    }

    public void close() {
        if (input != null) {
            try {
                input.inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
