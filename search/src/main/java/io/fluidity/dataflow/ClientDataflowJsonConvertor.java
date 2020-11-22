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

/**
 * Flips the server side ladder format to client side.
 */
public class ClientDataflowJsonConvertor {

    private final Long timeX1;
    private final Long timeX2;
    private final Long valueY;
    private Long granularityY;
    private List<String> urls = new ArrayList<>();

    public ClientDataflowJsonConvertor(Long timeX1, Long timeX2, Long valueY, Long granularityY) {
        this.timeX1 = timeX1;
        this.timeX2 = timeX2;
        this.valueY = valueY;
        this.granularityY = granularityY;
    }

    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
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
     * @return
     */
    public byte[] toJson(List<FlowInfo> flows) {
        try {
//            String json = "[\n" +
//                    "                                 [\"txn\", \"Register\"],\n" +
//                    "                                 [\"txn-1000\", 1000]\n" +
//                    "                               ]";
//            System.out.println(json);
            return getMapper().writeValueAsBytes(flows);
//            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];//e.toString();
        }
    }

    public FlowInfo[] fromJson(byte[] json) {
        try {
            return getMapper().readValue(json, new FlowInfo[0].getClass());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String getListingPrefix(Long timeX1) {
        return null;
    }

    public void process(String itemName, String itemUrl) {
        // split item name to get X and latency
        if (isMatch(itemName)) {
            this.urls.add(itemUrl);
        }
    }

    private boolean isMatch(String itemName) {
        String[] split = itemName.split(Model.DELIM);
        long from = Long.parseLong(split[split.length - Model.FROM_END_INDEX]);
        long to = Long.parseLong(split[split.length - Model.TO_END_INDEX]);
        if (from > timeX1 && from < timeX2) {
            long latency = to - from;
            return latency > valueY - granularityY && latency < valueY + granularityY;
        }
        return false;
    }

    public List<String> getFlowUrls() {
        return urls;
    }

    /**
     * Output as:  ['txn-1000', 1000, 400, 200,1000, 400],
     * @param flowInfo1
     * @return
     */
    public String rewriteToClientJson(FlowInfo flowInfo1) {
        StringBuilder results = new StringBuilder("[");
        results.append("'").append(flowInfo1.flowId).append("'");
        List<Long> durationsAsInterval = flowInfo1.getDurationsAsInterval();
        durationsAsInterval.forEach(duration -> results.append(",").append(duration));
        results.append("]");
        return results.toString();
    }
}
