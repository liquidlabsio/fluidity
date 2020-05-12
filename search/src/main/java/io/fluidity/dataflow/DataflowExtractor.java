package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluidity.search.Search;
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

import static io.fluidity.dataflow.Model.CORR_DAT_FMT;
import static io.fluidity.dataflow.Model.CORR_FILE_FMT;

/**
 * Extract correlated data to relevant files and store on CloudStorage Dataflow model directory.
 */
public class DataflowExtractor implements AutoCloseable {
    private final Logger log = LoggerFactory.getLogger(DataflowExtractor.class);

    private final InputStream input;
    private final StorageUtil storageUtil;
    private String modelPath;
    private final String region;
    private final String tenant;
    private int logWarningCount = 0;

    public DataflowExtractor(InputStream inputStream, StorageUtil storageUtil, String modelPath, String region, String tenant) {
        this.input = inputStream;
        this.storageUtil = storageUtil;
        this.modelPath = modelPath;
        this.region = region;
        this.tenant = tenant;
    }

    public String process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long fileLength, String timeFormat) throws IOException {

        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor(timeFormat);

        LinkedList<Integer> lengths = new LinkedList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(input)));
        String nextLine = reader.readLine();
        lengths.add(nextLine.length());
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long scanFilePos = 0;

        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine);

        File currentFile = null;
        OutputStream bos = null;
        String currentCorrelation = "nada";
        long startTime = 0;

        Map<String, KvJsonPairExtractor> extractorMap = getExtractorMap();
        Map<String, String> datData = new HashMap<>();
        try {

            while (nextLine != null) {
                if (search.matches(nextLine)) {
                    Pair<String, Long> fieldNameAndValue = search.getFieldNameAndValue("file-name-source", nextLine);

                    String correlationId = fieldNameAndValue.getLeft();
                    if (correlationId != null) {
                        if (!currentCorrelation.equals(correlationId)) {
                            if (bos != null) {
                                bos.close();
                                storageUtil.copyToStorage(new FileInputStream(currentFile), region, tenant, String.format(CORR_FILE_FMT, modelPath, correlationId, startTime, currentTime), 365, currentTime);
                                currentFile.delete();
                                storageUtil.copyToStorage(makeDatFile(datData), region, tenant, String.format(CORR_DAT_FMT, modelPath, correlationId, startTime, currentTime), 365, currentTime);
                                datData.clear();
                            }
                            currentCorrelation = correlationId;
                            currentFile = File.createTempFile(correlationId, ".log");
                            bos = new BufferedOutputStream(new FileOutputStream(currentFile));
                            startTime = currentTime;
                        }

                        getDatData(nextLine, datData, extractorMap);
                        if (bos != null) {
                            bos.write(nextLine.getBytes());
                            bos.write('\n');
                        }
                    }
                }

                // keep calibrating fake time calc based on location
                nextLine = reader.readLine();


                // recalibrate the time interval as more line lengths are known
                if (nextLine != null) {
                    lengths.add(nextLine.length());
                    guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, currentTime, fileToTime, fileLength, scanFilePos, lengths);
                    scanFilePos += nextLine.length() + 2;

                    currentTime = dateTimeExtractor.getTimeMaybe(currentTime, guessTimeInterval, nextLine);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (bos != null) {
                bos.close();
                storageUtil.copyToStorage(new FileInputStream(currentFile), region, tenant, String.format(CORR_FILE_FMT, modelPath, currentCorrelation, startTime, currentTime), 365, currentTime);
                storageUtil.copyToStorage(makeDatFile(datData), region, tenant, String.format(CORR_DAT_FMT, modelPath, currentCorrelation, startTime, currentTime), 365, currentTime);
                currentFile.delete();
            }
        }
        return "done";
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

    private void getDatData(String nextLine, Map<String, String> datData, Map<String, KvJsonPairExtractor> extractorMap) {
        extractorMap.values().stream().forEach(extractor -> {
            try {
                Pair<String, Long> extracted = extractor.getKeyAndValue("none", nextLine);
                if (extracted != null) {
                    datData.put(extractor.getToken(), extracted.getRight().toString());
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
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
