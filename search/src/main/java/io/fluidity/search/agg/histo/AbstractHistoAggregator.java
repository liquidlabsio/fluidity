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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.fluidity.search.Search;
import io.fluidity.util.PairDeserializer;
import org.apache.commons.io.IOUtils;
import org.graalvm.collections.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class AbstractHistoAggregator<T> implements HistoAggregator<T> {
    protected final Map<String, InputStream> inputStreams;
    protected final Search search;

    public AbstractHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        this.inputStreams = inputStreams;
        this.search = search;
    }

    @Override
    public String process() throws Exception {
        List<String> collectedJson = inputStreams.entrySet().stream().map(entry -> readJson(entry.getValue())).collect(Collectors.toList());

        // TODO: implement reduce functionality between each of the series, i.e. avg/stats/min/max etc
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pair.class, new PairDeserializer());
        objectMapper.registerModule(module);
        List<List> collectedSeriesList =
                collectedJson.stream().map(json -> {
                    try {
                        return objectMapper.readValue(json, new TypeReference<List<TimeSeries>>() {
                        });
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());

        List<Series> collectedSeriesWithPossibleDuplicateNames = (List<Series>) collectedSeriesList.stream().flatMap(list -> list.stream()).collect(Collectors.toList());

        HashMap<String, Series<T>> reducedSeries = new HashMap<>();
        collectedSeriesWithPossibleDuplicateNames.forEach(series ->
        {
            if (reducedSeries.containsKey(series.name())) {
                reducedSeries.get(series.name()).merge(series);
            } else {
                reducedSeries.put(series.name(), series);
            }
        }
        );

        return objectMapper.writeValueAsString(ClientFormatter.getHistoSeriesForClient(processSeries(reducedSeries.values())));
    }

    abstract List<Series<Long>> processSeries(Collection<Series<T>> collectedSeries);

    private String readJson(InputStream inputStream) {
        try {
            return new String(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        this.inputStreams.values().forEach(stream -> {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
