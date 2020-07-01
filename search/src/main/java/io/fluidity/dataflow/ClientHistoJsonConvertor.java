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

package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import org.graalvm.collections.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Flips the server side histo to client json format for upload
 * i.e.
 * {
 *   "groupBy" : "",
 *   "name" : "none",
 *   "data" : [ {
 *     "left" : 1591271280000, "right" : null }, { "left" : 1591271460000, "right" : null }, {
 *     "left" : 1591271640000,
 *     "right" : {
 *       "47100" : {
 *         "opLatency" : [ -43200, 0, 0 ],
 *         "opDuration" : [ 47143, 47143, 141429 ],
 *         "duration" : [ 47143, 47143, 141429 ],
 *         "count" : 3
 *       }
 *     }
 *   }, <etc>
 *
 *  TO: arrays of timestamp, min, max, avg, volume
 *   timestamp:   [ t, t+1, t+1, t+3 ... ]
 *   min:   [ 100, 200, 300, 400...]
 *   max:     [ 10, 25, 30, 10   ... ]
 *   avg:   [ 100, 250, 300, 100...]
 *   volume:     [ 10, 25, 30, 10....]
 *
 */
public class ClientHistoJsonConvertor {
    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pair.class, new PairDeserializer(Long.class, FlowStats.class));
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public byte[] toJson(Series<FlowStats> histo) {
        try {
            return getMapper().writeValueAsBytes(histo);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "e.toString".getBytes();
        }
    }
    public TimeSeries<FlowStats> fromJson(byte[] json) {
        try {
            return getMapper().readValue(json, new TimeSeries<FlowStats>().getClass());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public String toClientArrays(TimeSeries<FlowStats> timeSeries) {
        try {
            List<Long> times = new ArrayList<>();
            List<Long> min = new ArrayList<>();
            List<Long> max = new ArrayList<>();
            List<Long> avg = new ArrayList<>();
            List<Long> volume = new ArrayList<>();

            timeSeries.data().forEach(entry -> {
                Long timestamp = entry.getLeft();
                FlowStats flowStats = entry.getRight();
                if (flowStats == null) {
                    flowStats = new FlowStats();
                    flowStats.setDuration(new long[] { 0, 0, 0});

                }
                times.add(timestamp/1000);
                min.add(flowStats.getDuration()[0]);
                max.add(flowStats.getDuration()[1]);
                avg.add(flowStats.getCount() > 0 ? flowStats.getDuration()[2]/flowStats.getCount() : 0);
                volume.add(flowStats.getCount());
            });

            ArrayList<List<Long>> results = new ArrayList<>();
            results.add(times);
            results.add(min);
            results.add(avg);
            results.add(max);
            results.add(volume);

            return getMapper().writeValueAsString(results);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
