/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

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
import java.util.Optional;

/**
 * Note: lines must be written in following format: timestamp:filepos:data to the filtered view - the .event file
 * <p>
 * <p>
 */
public class SearchEventCollector implements EventCollector {
    private final HistoCollector histoCollector;
    private InputStream input;
    private OutputStream output;

    public SearchEventCollector(final HistoCollector histoCollector, final InputStream input,
                                final OutputStream output) {
        this.histoCollector = histoCollector;
        this.input = input;
        this.output = output;
    }

    @Override
    public Integer[] process(final boolean isCompressed, final Search search, final long fileFromTime,
                             final long fileToTime, final long fileLength, final String timeFormat) throws IOException {

        int readEvents = 0;
        int totalEvents = 0;

        final DateTimeExtractor dateTimeExtractor = new DateTimeExtractor(timeFormat);
        final BufferedOutputStream bos = new BufferedOutputStream(output);
        final BufferedInputStream bis = new BufferedInputStream(input);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
        long bytePosition = 0;

        final LinkedList<Integer> lengths = new LinkedList<>();

        Optional<String> nextLine = Optional.ofNullable(reader.readLine());
        if (nextLine.isPresent()) {
            lengths.add(nextLine.get().length());
        }
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long scanFilePos = 0;

        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine.get());
        try {

            while (nextLine.isPresent()) {

                if (currentTime > search.from && currentTime < search.to && search.matches(nextLine.get())) {
                    byte[] bytes = new StringBuilder().append(currentTime).append(':').append(bytePosition).append(':')
                            .append(nextLine.get()).append('\n').toString().getBytes();
                    bos.write(bytes);
                    histoCollector.add(currentTime, bytePosition, nextLine.get());
                    readEvents++;
                    readEvents++;// NL

                    // tracks the dest file offset - so it can be seek-to-offset for user actions (histogram click, or raw events click)
                    bytePosition += bytes.length;
                }

                // keep calibrating fake time calc based on location
                nextLine = Optional.ofNullable(reader.readLine());


                // recalibrate the time interval as more line lengths are known
                if (nextLine.isPresent()) {
                    String line = nextLine.get();
                    int length = nextLine.get().length();
                    lengths.add(length);
                    guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, currentTime, fileToTime, fileLength, scanFilePos, lengths);
                    scanFilePos += length + 2;

                    currentTime = dateTimeExtractor.getTimeMaybe(currentTime, guessTimeInterval, line);
                }
                totalEvents++;
            }
            bos.flush();
        } finally {
            reader.close();
            bos.close();
        }
        return new Integer[]{readEvents, totalEvents};
    }

    @Override
    public void close() throws Exception {
    }
}
