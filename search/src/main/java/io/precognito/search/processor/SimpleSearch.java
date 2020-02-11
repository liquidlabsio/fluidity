package io.precognito.search.processor;

import io.precognito.search.Search;

import java.io.*;
import java.util.Scanner;

/**
 * Grep or Filter lines according to the search expression.
 * TODO: look at native TruffleRegEx in Graalvm & Quarkus
 * https://www.graalvm.org/docs/reference-manual/native-image/
 * Macro Options
 *
 * Macro-options are mainly helpful for polyglot capabilities of native images:
 *
 * --language:regex to make Truffle Regular Expression engine available that exposes regular expression functionality in GraalVM supported languages
 */
public class SimpleSearch implements Processor {
    private InputStream input;
    private OutputStream output;

    @Override
    public int process(Search search, InputStream input, OutputStream output, long fromTime, long toTime, long length) throws IOException {
        this.input = input;
        this.output = output;

        int read = 0;

        BufferedOutputStream bos = new BufferedOutputStream(output);
        BufferedInputStream bis = new BufferedInputStream(input);
        Scanner scanner = new Scanner(bis);
        long position = 0;
        long startingTime = fromTime;
        long guessTimeInterval = guessTimeInterval(fromTime, toTime, length);
        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();

            if (search.matches(nextLine)) {
                bos.write(Long.toString(startingTime).getBytes());
                bos.write(':');
                bos.write(Long.toString(position).getBytes());
                bos.write(':');
                bos.write(nextLine.getBytes());
                bos.write('\n');
                read++;
                read++;// NL
            }
            startingTime += guessTimeInterval;
            position += nextLine.length();
        }
        bos.flush();
        return read;
    }

    /**
     * Fudge the time interval from the time-span and the size of the file - presume avg line length is 1024 bytes.
     * Very hacky - but saves reading the file;
     * @param fromTime
     * @param toTime
     * @param length
     * @return
     */
    private long guessTimeInterval(long fromTime, long toTime, long length) {
        // presume average line length = 1024 bytes;
        long guessedLineCount = length/1024;
        return (toTime - fromTime)/guessedLineCount;
    }

    @Override
    public void close() throws Exception {
        if (input !=null) input.close();
        if (output !=null) output.close();
    }
}
