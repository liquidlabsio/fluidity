package io.fluidity.search.agg.events;

import io.fluidity.search.Search;
import io.fluidity.search.agg.histo.HistoCollector;
import io.fluidity.util.DateTimeExtractor;

import java.io.*;
import java.util.LinkedList;

/**
 * Note: lines must be written in following format: timestamp:filepos:data to the filtered view - the .event file
 * <p>
 * Grep or Filter lines according to the search expression.
 * TODO: look at native TruffleRegEx in Graalvm & Quarkus
 * https://www.graalvm.org/docs/reference-manual/native-image/
 * <p>
 * Macro-options are mainly helpful for polyglot capabilities of native images:
 * <p>
 * --language:regex to make Truffle Regular Expression engine available that exposes regular expression functionality in GraalVM supported languages
 */
public class SearchEventCollector implements EventCollector {
    private InputStream input;
    private OutputStream output;

    @Override
    public int[] process(boolean isCompressed, HistoCollector histoCollector, Search search, InputStream input, OutputStream output, long fileFromTime, long fileToTime, long fileLength, String timeFormat) throws IOException {
        this.input = input;
        this.output = output;

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
        long guessTimeInterval = guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
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
                guessTimeInterval = guessTimeInterval(isCompressed, currentTime, fileToTime, fileLength, scanFilePos, lengths);
                scanFilePos += nextLine.length() + 2;

                currentTime = dateTimeExtractor.getTimeMaybe(currentTime, guessTimeInterval, nextLine);
            }
            totalEvents++;
        }
        bos.flush();
        return new int[] { readEvents, totalEvents };
    }

    /**
     * Fudge the time interval from the time-span and the size of the file - presume avg line fileLength is 1024 bytes.
     * Hacky - but very fast.
     *
     * @param fromTime
     * @param toTime
     * @param fileLength
     * @return
     */
    private long guessTimeInterval(boolean isCompressed, long fromTime, long toTime, long fileLength, long currentPos, LinkedList<Integer> lengths) {
        if (isCompressed) {
            fileLength *= 100;
        }
        if (lengths.size() > 100) lengths.pop();
        // presume average line fileLength = 1024 bytes;
        long recentLengthSum = lengths.stream().mapToInt(Integer::intValue).sum();
        int avgRecentLength = (int) (recentLengthSum / lengths.size());
        long guessedLineCount = avgRecentLength > 1024 ? (fileLength - currentPos) / avgRecentLength : 10;
        if (guessedLineCount == 0) guessedLineCount = 10;
        return (toTime - fromTime) / guessedLineCount;
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
    }
}
