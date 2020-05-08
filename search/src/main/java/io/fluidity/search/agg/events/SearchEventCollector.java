package io.fluidity.search.agg.events;

import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoCollector;
import io.fluidity.util.DateTimeExtractor;
import io.fluidity.util.DateUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Note: lines must be written in following format: timestamp:filepos:data to the filtered view - the .event file
 * <p>
 * <p>
 */
public class SearchEventCollector implements EventCollector {
    private final HistoCollector histoCollector;
    private InputStream input;
    private OutputStream output;

    public SearchEventCollector(HistoCollector histoCollector, InputStream input, OutputStream output) {
        this.histoCollector = histoCollector;
        this.input = input;
        this.output = output;
    }

    @Override
    public int[] process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long fileLength, String timeFormat) throws IOException {

        int readEvents = 0;
        int totalEvents = 0;

        DateTimeExtractor dateTimeExtractor = new DateTimeExtractor(timeFormat);
        BufferedOutputStream bos = new BufferedOutputStream(output);
        BufferedInputStream bis = new BufferedInputStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
        long position = 0;

        LinkedList<Integer> lengths = new LinkedList<>();

        String nextLine = reader.readLine();
        lengths.add(nextLine.length());
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long scanFilePos = 0;

        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine);

        while (nextLine != null) {

            if (currentTime > search.from && currentTime < search.to && search.matches(nextLine)) {
                byte[] bytes = new StringBuilder().append(currentTime).append(':').append(position).append(':').append(nextLine).append('\n').toString().getBytes();
                bos.write(bytes);
                histoCollector.add(currentTime, position, nextLine);
                readEvents++;
                readEvents++;// NL

                // tracks the dest file offset - so it can be seek-to-offset for user actions (histogram click, or raw events click)
                position += bytes.length;
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
            totalEvents++;
        }
        bos.flush();
        return new int[] { readEvents, totalEvents };
    }

    @Override
    public void close() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            histoCollector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
