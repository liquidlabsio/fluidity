/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.search.agg.histo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fluidity.search.Search;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Pair;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 * Generic collection of data and points into series and timeseries mapping
 *
 *   [
 *     {
 *       name: "Series 1",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     },
 *      {
 *       name: "Series 2",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     }
 *   ]
 */
public class SimpleHistoCollector implements HistoCollector {
    private final long from;
    private final long to;
    private final HistoFunction<Long, Long> function;
    private String sourceName;
    private OutputStream outputStream;
    private String tags;
    private Search search;
    private final EconomicMap<String, Series<Long>> seriesMap = EconomicMap.create();

    public SimpleHistoCollector(OutputStream outputStream, Search search, long from, long to, HistoFunction<Long, Long> histoFunction) {
        this.outputStream = outputStream;
        this.search = search;
        this.from = from;
        this.to = to;
        this.function = histoFunction;
    }

    @Override
    public void updateFileInfo(String filename, String tags) {
        this.sourceName = filename;
        this.tags = tags;
    }

    @Override
    public void add(long currentTime, long bytePosition, String nextLine) {

        Pair<String, Long> seriesNameAndValue = search.getFieldNameAndValue(sourceName, nextLine);
        if (seriesNameAndValue != null) {
            String groupBy = search.applyGroupBy(tags, sourceName);
            seriesNameAndValue = Pair.create(groupBy + "-" + seriesNameAndValue.getLeft(), seriesNameAndValue.getRight());
            Series<Long> series = getSeriesItem(groupBy, seriesNameAndValue.getLeft());
            Long calculate = function.calculate(series.get(currentTime), seriesNameAndValue.getRight(), nextLine, bytePosition, currentTime, series.index(currentTime), search.expression);
            series.update(currentTime, calculate);
        }
    }

    private Series<Long> getSeriesItem(String groupBy, String seriesName) {
        if (!seriesMap.containsKey(seriesName)){
            seriesMap.put(seriesName, search.getTimeSeries(seriesName, groupBy, from, to));
        }
        return seriesMap.get(seriesName);
    }

    @Override
    public void close() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            List<Series> seriesList = StreamSupport.stream(seriesMap.getValues().spliterator(), false).collect(Collectors.toList());
            String histoJson = objectMapper.writeValueAsString(new ArrayList(seriesList));
            outputStream.write(histoJson.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EconomicMap<String, Series<Long>> series() {
        return seriesMap;
    }
}
