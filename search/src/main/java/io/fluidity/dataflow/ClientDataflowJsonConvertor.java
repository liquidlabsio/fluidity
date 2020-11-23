/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software  distributed under the License is
 *  distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Flips the server side ladder format to client side.
 */
public class ClientDataflowJsonConvertor {

    private final Long timeX1;
    private final Long timeX2;
    private final Long valueY;
    private Long granularityY;

    public ClientDataflowJsonConvertor(final Long timeX1, final Long timeX2, final Long valueY,
                                       final Long granularityY) {
        this.timeX1 = timeX1;
        this.timeX2 = timeX2;
        this.valueY = valueY;
        this.granularityY = granularityY;
    }

    private ObjectMapper getMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
//        module.addDeserializer(FlowInfo.class);
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * converts FlowInfo's into -> [
     *                                  ['id', 'component-a.dostuff', 'component-b.thinking'],
     *                                  ['txn-1000', 1000, 400, 200,1000, 400],
     *                                  ['txn-1222', 1170, 460, 250, 1000, 400]
     *                                ];
     *  See trial/gchart/stacked-bar
     * @return
     */
    public byte[] toClientFlowsList(final List<FlowInfo> flows) {
        try {
            List<List<Object>> allflows = flows.stream().map(flow -> flow.durations()).collect(Collectors.toList());

            final AtomicLong columnCount = new AtomicLong(1L);
            allflows.forEach(row -> columnCount.set(Math.max(columnCount.get(), row.size())));

            // make durations lists the same length
            alignSizesForColumnCount(columnCount.intValue(), allflows);

            sortByDuration(allflows);


            List<Object> columnNames = new ArrayList<>();
            columnNames.add("id");
            for (int i = 0; i < columnCount.intValue()-1; i++) {
                columnNames.add("stage-" + i);
            }
            allflows.add(0, columnNames);
            return getMapper().writeValueAsBytes (allflows);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private void sortByDuration(List<List<Object>> allflows) {
        allflows.sort((o1, o2) -> {
            Long o1Sum = o1.stream().filter(item -> item instanceof Long).mapToLong(item -> (Long) item).sum();
            Long o2Sum = o2.stream().filter(item -> item instanceof Long).mapToLong(item -> (Long) item).sum();
            return o2Sum.compareTo(o1Sum);
        });
    }

    private void alignSizesForColumnCount(final int intValue, final List<List<Object>> allflows) {
        allflows.forEach(flowList -> {
            while (flowList.size() < intValue) {
                flowList.add(1L);
            }
        });
    }

    public FlowInfo[] fromJson(final byte[] json) {
        try {
            return getMapper().readValue(json, new FlowInfo[0].getClass());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isMatch(final String itemName) {
        final String[] split = itemName.split(Model.DELIM);
        final long from = Long.parseLong(split[split.length - Model.FROM_END_INDEX]);
        final long to = Long.parseLong(split[split.length - Model.TO_END_INDEX]);
        // overlapping times
        if (from <= timeX2 && from >= timeX1 || to <= timeX2 && to >= timeX1 ||
        from <= timeX1 && to >= timeX2) {
            final long latency = to - from;
            return latency > valueY - granularityY && latency < valueY + granularityY || granularityY == -1;
        }
        return false;
    }

}
