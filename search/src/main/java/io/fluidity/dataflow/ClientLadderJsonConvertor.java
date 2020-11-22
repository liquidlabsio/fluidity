/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed
 *   on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.fluidity.dataflow.histo.FlowStats;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import org.graalvm.collections.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flips the server side ladder format to client side.
 */
public class ClientLadderJsonConvertor {

    private ObjectMapper getMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(Pair.class, new PairDeserializer(Long.class, new TypeReference<HashMap<Long, FlowStats>>() { } ));
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public byte[] toJson(final Series<Map<Long, FlowStats>> histo) {
        try {
            return getMapper().writeValueAsBytes(histo);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "e.toString".getBytes();
        }
    }
    public TimeSeries<Map<Long, FlowStats>> fromJson(final byte[] json) {
        try {
            return getMapper().readValue(json, new TimeSeries<Map<Long, FlowStats>>().getClass());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public String toClientArrays(final TimeSeries<Map<Long, FlowStats>> timeSeries) {
        try {
            final List<Long> times = new ArrayList<>();
            final List<List<Long>> yValue = new ArrayList<>();
            final List<List<Long>> yCount = new ArrayList<>();

            timeSeries.data().forEach(entry -> {
                final Long timestamp = entry.getLeft();
                times.add(timestamp/1000);
                Map<Long, FlowStats> flowStats = entry.getRight();
                if (flowStats == null) {
                    flowStats = new HashMap<>();
                }
                final List<Long> thisYVals = new ArrayList<>();
                final List<Long> thisYCounts = new ArrayList<>();
                flowStats.entrySet().forEach(flowStatsEntry -> {
                    thisYVals.add(flowStatsEntry.getKey());
                    thisYCounts.add(flowStatsEntry.getValue().getCount());
                });
                yValue.add(thisYVals);
                yCount.add(thisYCounts);
            });

            final ArrayList<List> results = new ArrayList<>();
            results.add(times);
            results.add(yValue);
            results.add(yCount);
            return getMapper().writeValueAsString(results);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
