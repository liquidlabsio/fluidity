/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

        Optional<String> nextLine = Optional.ofNullable(reader.readLine());
        if (nextLine.isPresent()) lengths.add(nextLine.get().length());
        long guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, fileFromTime, fileToTime, fileLength, 0, lengths);
        long scanFilePos = 0;

        long currentTime = dateTimeExtractor.getTimeMaybe(fileFromTime, guessTimeInterval, nextLine);
        try {

            while (nextLine.isPresent()) {

                if (currentTime > search.from && currentTime < search.to && search.matches(nextLine.get())) {
                    byte[] bytes = new StringBuilder().append(currentTime).append(':').append(position).append(':').append(nextLine).append('\n').toString().getBytes();
                    bos.write(bytes);
                    histoCollector.add(currentTime, position, nextLine.get());
                    readEvents++;
                    readEvents++;// NL

                    // tracks the dest file offset - so it can be seek-to-offset for user actions (histogram click, or raw events click)
                    position += bytes.length;
                }

                // keep calibrating fake time calc based on location
                nextLine = Optional.ofNullable(reader.readLine());


                // recalibrate the time interval as more line lengths are known
                if (nextLine.isPresent()) {
                    lengths.add(nextLine.get().length());
                    guessTimeInterval = DateUtil.guessTimeInterval(isCompressed, currentTime, fileToTime, fileLength, scanFilePos, lengths);
                    scanFilePos += nextLine.get().length() + 2;

                    currentTime = dateTimeExtractor.getTimeMaybe(currentTime, guessTimeInterval, nextLine);
                }
                totalEvents++;
            }
            bos.flush();
        } finally {
            reader.close();
            bos.close();
        }
        return new int[] { readEvents, totalEvents };
    }

    @Override
    public void close() throws Exception {
        histoCollector.close();
    }
}
