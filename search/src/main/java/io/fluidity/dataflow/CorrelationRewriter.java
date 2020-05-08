package io.fluidity.dataflow;

import io.fluidity.search.Search;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.search.field.extractor.KvJsonPairExtractor;
import io.fluidity.util.DateTimeExtractor;
import io.fluidity.util.DateUtil;
import org.graalvm.collections.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Extract correlated data to relevant files and store on CloudStorage Dataflow model directory.
 */
public class CorrelationRewriter implements AutoCloseable {

    public final static String CORR_PREFIX = "/corr-";
    // correlation-start-end-events
    public final static String CORR_FILE_FMT = "%s/corr-%s-%d-%d-%d.log";

    private final InputStream input;
    private final StorageUtil storageUtil;
    private String filePrefix;
    private final String region;
    private final String tenant;

    public CorrelationRewriter(InputStream inputStream, StorageUtil storageUtil, String filePrefix, String region, String tenant) {
        this.input = inputStream;
        this.storageUtil = storageUtil;
        this.filePrefix = filePrefix;
        this.region = region;
        this.tenant = tenant;
    }

    public String process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long fileLength, String timeFormat) throws IOException {

        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor(timeFormat);

        BufferedInputStream bis = new BufferedInputStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis));

        LinkedList<Integer> lengths = new LinkedList<>();

        String nextLine = reader.readLine();
        lengths.add(nextLine.length());
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long scanFilePos = 0;

        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine);

        File currentFile = null;
        OutputStream bos = null;
        String currentCorrelation = "nada";
        long startTime = 0;

        // look for json information about which stage of a trace or the name of the service being processed
        // loginService
        KvJsonPairExtractor service = new KvJsonPairExtractor("service");
        // doStuff
        KvJsonPairExtractor operation = new KvJsonPairExtractor("operation");
        // REST, SQL, Lambda, Micro-thingy
        KvJsonPairExtractor type = new KvJsonPairExtractor("type");
        // anthing else that is useful
        KvJsonPairExtractor meta = new KvJsonPairExtractor("meta");
        try {

            while (nextLine != null) {
                if (search.matches(nextLine)) {
                    Pair<String, Object> fieldNameAndValue = search.getFieldNameAndValue("file-name-source", nextLine);
                    String correlationId = fieldNameAndValue.getLeft();
                    if (correlationId != null) {
                        if (!currentCorrelation.equals(correlationId)) {
                            if (bos != null) {
                                bos.close();
                                storageUtil.copyToStorage(currentFile, region, tenant, String.format(CORR_FILE_FMT, filePrefix, correlationId, startTime, currentTime), 365, currentTime);
                            }
                            currentCorrelation = correlationId;
                            currentFile = File.createTempFile(correlationId, ".log");
                            bos = new BufferedOutputStream(new FileOutputStream(currentFile));
                            startTime = currentTime;
                        }
                    }
                    if (bos != null) {
                        bos.write(nextLine.getBytes());
                        bos.write('\n');
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
            if (bos != null) {
                bos.close();
                storageUtil.copyToStorage(currentFile, region, tenant, String.format(CORR_FILE_FMT, filePrefix, currentCorrelation, startTime, currentTime), 365, currentTime);
            }
        }
        return "done";
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
