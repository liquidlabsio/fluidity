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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.fluidity.search.Search;
import io.fluidity.search.StorageInputStream;
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
    protected final Map<String, StorageInputStream> inputStreams;
    protected final Search search;

    public AbstractHistoAggregator(Map<String, StorageInputStream> inputStreams, Search search) {
        this.inputStreams = inputStreams;
        this.search = search;
    }

    @Override
    public String process() throws Exception {
        List<String> collectedJson = inputStreams.entrySet().stream().map(entry -> readJson(entry.getValue().inputStream)).collect(Collectors.toList());

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
                stream.inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
