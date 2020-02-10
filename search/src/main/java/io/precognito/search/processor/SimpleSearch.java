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
    public int process(Search search, InputStream input, OutputStream output) throws IOException {
        this.input = input;
        this.output = output;

        int read = 0;

        BufferedOutputStream bos = new BufferedOutputStream(output);
        BufferedInputStream bis = new BufferedInputStream(input);
        Scanner scanner = new Scanner(bis);
        long position = 0;
        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();

            if (search.matches(nextLine)) {
                bos.write(Long.toString(System.currentTimeMillis()).getBytes());
                bos.write(':');
                bos.write(Long.toString(position).getBytes());
                bos.write(':');
                bos.write(nextLine.getBytes());
                bos.write('\n');
                read++;
                read++;// NL
            }
            position += nextLine.length();
        }
        bos.flush();
        return read;
    }

    @Override
    public void close() throws Exception {
        if (input !=null) input.close();
        if (output !=null) output.close();
    }
}
