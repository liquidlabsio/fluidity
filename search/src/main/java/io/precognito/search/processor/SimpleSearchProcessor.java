package io.precognito.search.processor;

import io.precognito.search.Search;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: lines must be written in following format: timestamp:filepos:data
 *
 * Grep or Filter lines according to the search expression.
 * TODO: look at native TruffleRegEx in Graalvm & Quarkus
 * https://www.graalvm.org/docs/reference-manual/native-image/
 * Macro Options
 *
 * Macro-options are mainly helpful for polyglot capabilities of native images:
 *
 * --language:regex to make Truffle Regular Expression engine available that exposes regular expression functionality in GraalVM supported languages
 */
public class SimpleSearchProcessor implements Processor {
    private InputStream input;
    private OutputStream output;

    @Override
    public int process(boolean isCompressed, HistoCollector histoCollector, Search search, InputStream input, OutputStream output, long fileFromTime, long fileToTime, long length) throws IOException {
        this.input = input;
        this.output = output;

        int read = 0;

        BufferedOutputStream bos = new BufferedOutputStream(output);
        BufferedInputStream bis = new BufferedInputStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
        long position = 0;
        long currentTime = fileFromTime;

        ArrayList<Integer> lengths = new ArrayList<>();

        String nextLine = reader.readLine();
        lengths.add(nextLine.length());
        long guessTimeInterval = guessTimeInterval(isCompressed, fileFromTime, fileToTime, length, lengths);

        while (nextLine != null) {

            if (currentTime > search.from && currentTime < search.to && search.matches(nextLine)) {
                bos.write(Long.toString(currentTime).getBytes());
                bos.write(':');
                bos.write(Long.toString(position).getBytes());
                bos.write(':');
                bos.write(nextLine.getBytes());
                bos.write('\n');
                histoCollector.add(currentTime, position, nextLine);
                read++;
                read++;// NL
            }
            currentTime += guessTimeInterval;
            position += nextLine.length();
            // keep calibrating fake time calc based on location
            nextLine = reader.readLine();
            if (nextLine != null) {
                lengths.add(nextLine.length());
                guessTimeInterval = guessTimeInterval(isCompressed, currentTime, fileToTime, length, lengths);
            }
        }
        bos.flush();
        return read;
    }

    /**
     * Fudge the time interval from the time-span and the size of the file - presume avg line length is 1024 bytes.
     * Very hacky - but saves reading the file;
     *
     * @param fromTime
     * @param toTime
     * @param length
     * @return
     */
    private long guessTimeInterval(boolean isCompressed, long fromTime, long toTime, long length, List<Integer> lengths) {
        if (isCompressed) {
            length *= 100;
        }
        // presume average line length = 1024 bytes;
        long currentPos = lengths.stream().mapToInt(Integer::intValue).sum();
        int avgLength = (int) (currentPos / lengths.size());
        long guessedLineCount = avgLength > 1024 ? (length - currentPos) / avgLength : 10;
        if (guessedLineCount == 0) guessedLineCount = 10;
        return (toTime - fromTime) / guessedLineCount;
    }

    @Override
    public void close() throws Exception {
        if (input !=null) input.close();
        if (output !=null) output.close();
    }
}
