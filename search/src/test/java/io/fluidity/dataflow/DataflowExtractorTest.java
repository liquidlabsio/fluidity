package io.fluidity.dataflow;

import io.fluidity.search.Search;
import io.fluidity.search.agg.events.StorageUtil;
import io.fluidity.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataflowExtractorTest {

    @Test
    void processWithCorrelationEnhancements() throws IOException {
        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.toString().getBytes());
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(inputStream, baos);
                collected.put(filePath, baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        DataflowExtractor rewriter = new DataflowExtractor(instream, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0;
        search.to = System.currentTimeMillis();

        rewriter.process(false, search, 0, System.currentTimeMillis(), 1024, "");
        assertEquals(2, collected.size());
        List<Map.Entry<String, String>> datFile = collected.entrySet().stream().filter(entry -> entry.getKey().endsWith(".dat")).collect(Collectors.toList());
        String jsonDatData = datFile.iterator().next().getValue();
        assertEquals(5, jsonDatData.split(":").length, "JSON spit by : didnt provide 5 segments json was:" + jsonDatData);
    }

    @Test
    void process() throws IOException {
        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.toString().getBytes());
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = (inputStream, region, tenant, filePath, daysRetention, lastModified) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(inputStream, baos);
                collected.put(filePath, baos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
        DataflowExtractor rewriter = new DataflowExtractor(instream, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0;
        search.to = System.currentTimeMillis();

        rewriter.process(false, search, 0, System.currentTimeMillis(), 1024, "");
        assertEquals(2, collected.size());
        String s = collected.values().iterator().next();
        assertTrue(s.split("\n").length > 10, " Got bad text:" + s);
    }

    private StringBuilder makeFileContent() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");
        StringBuilder fileContentAsString = new StringBuilder();
        String data = "\"timestamp\":1587210348,\"txn\":\"99kerob\",\"service\":\"rest\", \"operation\":\"myOp\"" +
                "\"type\":\"microservice\", \"meta\":\"errors\"";

        long time = System.currentTimeMillis() - DateUtil.MINUTE;

        for (int i = 10; i < 100; i++) {
            fileContentAsString.append(dateTimeFormatter.print(time)).append(" ");
            fileContentAsString.append(data);
            fileContentAsString.append('\n');
            time += 1000;
        }
        return fileContentAsString;
    }

}