package io.precognito.search.processor;

import com.google.common.io.LineReader;
import io.precognito.search.Search;

import java.io.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        BufferedInputStream bis = new BufferedInputStream(input);
        Scanner scanner = new Scanner(bis);
        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();
            if (search.matches(nextLine)) {
                output.write(Long.toString(System.currentTimeMillis()).getBytes());
                output.write(':');
                output.write(nextLine.getBytes());
                output.write('\n');
                read++;
            }
        }
        return read;

    }

    @Override
    public void close() throws Exception {
        if (input !=null) input.close();
        if (output !=null) output.close();
    }
}
