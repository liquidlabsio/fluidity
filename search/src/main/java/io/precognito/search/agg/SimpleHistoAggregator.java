package io.precognito.search.agg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.precognito.search.Search;
import io.precognito.search.processor.Series;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleHistoAggregator implements HistoAggregator {
    private final Map<String, InputStream> inputStreams;
    private final Search search;

    public SimpleHistoAggregator(Map<String, InputStream> inputStreams, Search search) {
        this.inputStreams = inputStreams;
        this.search = search;
    }

    @Override
    public String process() throws Exception {
        List<String> collectedJson = inputStreams.entrySet().stream().map(entry -> readJson(entry.getValue())).collect(Collectors.toList());

        // TODO: implement reduce functionality between each of the series, i.e. avg/stats/min/max etc
        ObjectMapper objectMapper = new ObjectMapper();
        List<Series> collectedSeries = collectedJson.stream().map(json -> {
            try {
                return objectMapper.readValue(json, Series.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(series -> series.hasData()).collect(Collectors.toList());
        return objectMapper.writeValueAsString(collectedSeries);

    }

    private String readJson(InputStream inputStream){
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
    public void close() throws Exception {

    }
}
