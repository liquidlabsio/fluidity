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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationRewriterTest {

    @Test
    void process() throws IOException {
        StringBuilder fileContentAsString = makeFileContent();
        ByteArrayInputStream instream = new ByteArrayInputStream(fileContentAsString.toString().getBytes());
        final Map<String, String> collected = new HashMap<>();

        StorageUtil outFactory = new StorageUtil() {
            @Override
            public void copyToStorage(File currentFile, String region, String tenant, String filePath, int daysRetention, long lastModified) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    IOUtils.copy(new FileInputStream(currentFile), baos);
                    collected.put(filePath, baos.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        CorrelationRewriter rewriter = new CorrelationRewriter(instream, outFactory, "filePrefix", "region", "tenant");

        Search search = new Search();
        search.expression = "*|*|*|field.getJsonPair(txn)";
        search.from = 0;
        search.to = System.currentTimeMillis();

        rewriter.process(false, search, 0, System.currentTimeMillis(), 1024, "");
        assertEquals(1, collected.size());
        String s = collected.values().iterator().next();
        assertTrue(s.split("\n").length > 10);
    }

    private StringBuilder makeFileContent() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");
        StringBuilder fileContentAsString = new StringBuilder();
        String data = "\"timestamp\":1587210348,\"txn\":\"99kerob\",\"bot\":false,\"minor\":false,";

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