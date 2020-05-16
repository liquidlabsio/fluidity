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

package io.fluidity.search.agg.histo;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String sourceName;
    private OutputStream outputStream;
    private final String tags;
    private Search search;
    private final EconomicMap<String, Series<Long>> seriesMap = EconomicMap.create();

    public SimpleHistoCollector(OutputStream outputStream, String sourceName, String tags, Search search, long from, long to, HistoFunction<Long, Long> histoFunction) {
        this.outputStream = outputStream;
        this.tags = tags;
        this.search = search;
        this.sourceName = sourceName;
        this.from = from;
        this.to = to;
        this.function = histoFunction;
    }

    @Override
    public void add(long currentTime, long position, String nextLine) {

        Pair<String, Long> seriesNameAndValue = search.getFieldNameAndValue(sourceName, nextLine);
        if (seriesNameAndValue != null) {
            String groupBy = search.applyGroupBy(tags, sourceName);
            seriesNameAndValue = Pair.create(groupBy + "-" + seriesNameAndValue.getLeft(), seriesNameAndValue.getRight());
            Series<Long> series = getSeriesItem(groupBy, seriesNameAndValue.getLeft());
            Long calculate = function.calculate(series.get(currentTime), seriesNameAndValue.getRight(), nextLine, position, currentTime, series.index(currentTime), search.expression);
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
