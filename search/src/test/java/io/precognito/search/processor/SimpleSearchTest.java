package io.precognito.search.processor;

import io.precognito.search.Search;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleSearchTest {

    @Test
    public void testSearchGrep() throws Exception {

        StringBuilder fileContentAsString = makeFileContent();

        SimpleSearch simpleSearch = new SimpleSearch();
        Search search = new Search();
        search.expression = "* | * | * | * | CPU | *";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int process = simpleSearch.process(search, new ByteArrayInputStream(fileContentAsString.toString().getBytes()), baos, 0, System.currentTimeMillis(), 1024);
        assertTrue(process > 0);
        System.out.println("Processed:" + process);
        String outFileContents = new String(baos.toByteArray());
        System.out.println(outFileContents);
    }

    private StringBuilder makeFileContent() throws InterruptedException {
        StringBuilder fileContentAsString = new StringBuilder();
        for (int i = 0; i< 100; i++) {
            fileContentAsString.append(String.format("%s %s CPU:%d", System.currentTimeMillis(), i % 2 == 0 ?"ERROR" : "INFO", i));
            fileContentAsString.append('\n');
            Thread.sleep(10);
        }
        return fileContentAsString;
    }

}