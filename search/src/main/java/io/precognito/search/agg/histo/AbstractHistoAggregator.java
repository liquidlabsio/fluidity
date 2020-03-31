package io.precognito.search.agg.histo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class AbstractHistoAggregator implements HistoAggregator {
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

        List<Series> collectedSeries = (List<Series>) collectedSeriesList.stream().flatMap(list -> list.stream()).collect(Collectors.toList());

        return objectMapper.writeValueAsString(processSeries(collectedSeries));

    }

    abstract List<Series> processSeries(List<Series> collectedSeries);

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
