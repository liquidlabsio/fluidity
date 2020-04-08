package io.precognito.search.agg.events;

import io.precognito.search.Search;
import io.precognito.search.agg.histo.NoopHistoCollector;
import io.precognito.util.DateUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchEventCollectorTest {


        @Test
    public void testGenerateTestData() throws Exception {

        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url);
            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        (inboundSseEvent) -> {
                            System.out.println(inboundSseEvent + "\n\n");
                        });
             //   eventSource.register(onEvent, onError, onComplete);
                eventSource.open();

                //Consuming events for one hour
                Thread.sleep(60 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.close();
            System.out.println("End");




            String prefix = "/Volumes/SSD2/logs/precog-logs/test-cpu-";
            FileOutputStream fos = new FileOutputStream(prefix + new Date().getTime() + ".log");

            DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");

            long time = System.currentTimeMillis() - (DateUtil.DAY * 7);
            int minutes = (int) ((System.currentTimeMillis() - time) / DateUtil.MINUTE);
            int max = 100000;
            for (int i = 1; i < minutes; i++) {
                double cpu = (i / (double) minutes) * 100.0 + (10 * Math.random());
                fos.write(String.format("%s INFO CPU:%d\n", dateTimeFormatter.print(time), Double.valueOf(cpu).intValue()).getBytes());
                time += DateUtil.MINUTE;
            }
            fos.close();
        }


    @Test
    public void testSearchGetsTime() throws Exception {

        StringBuilder fileContentAsString = makeFileContent();

        SearchEventCollector simpleSearchProcessor = new SearchEventCollector();
        Search search = new Search();
        search.expression = "* | * | * | * | CPU | *";
        search.from = 0;
        search.to = System.currentTimeMillis();

        String timeFormat = "yyyy-MM-dd HH:mm.SS";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int process = simpleSearchProcessor.process(false, new NoopHistoCollector(), search, new ByteArrayInputStream(fileContentAsString.toString().getBytes()), baos, 0, System.currentTimeMillis(), 1024, timeFormat);
        assertTrue(process > 0, "didnt process any data");
        System.out.println("Processed:" + process);
        String outFileContents = new String(baos.toByteArray());
        System.out.println(outFileContents);
    }


    @Test
    public void testSearchGrep() throws Exception {

        StringBuilder fileContentAsString = makeFileContent();

        SearchEventCollector simpleSearchProcessor = new SearchEventCollector();
        Search search = new Search();
        search.expression = "* | * | * | * | CPU | *";
        search.from = 0;
        search.to = System.currentTimeMillis();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int process = simpleSearchProcessor.process(false, new NoopHistoCollector(), search, new ByteArrayInputStream(fileContentAsString.toString().getBytes()), baos, 0, System.currentTimeMillis(), 1024, "");
        assertTrue(process > 0, "didnt process any data");
        System.out.println("Processed:" + process);
        String outFileContents = new String(baos.toByteArray());
        System.out.println(outFileContents);
    }

    private StringBuilder makeFileContent() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS");
        StringBuilder fileContentAsString = new StringBuilder();

        long time = System.currentTimeMillis() - DateUtil.MINUTE;

        for (int i = 10; i < 100; i++) {
            fileContentAsString.append(String.format("%s %s CPU:%d", dateTimeFormatter.print(time), i % 2 == 0 ? "ERROR" : "INFO", i));
            fileContentAsString.append('\n');
            time += 1000;
        }
        return fileContentAsString;
    }

}