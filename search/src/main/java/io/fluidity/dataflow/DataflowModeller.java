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

import org.graalvm.collections.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents stages 2 and 3 from: https://github.com/liquidlabsio/fluidity/issues/50
 */
public class DataflowModeller {
    public DataflowModeller() {
    }

    public FlowInfo getCorrelationFlow(String flowId, List<Pair<Long, String>> correlationSet) {
        List<Long[]> durations = getDurations(correlationSet);
        return new FlowInfo(flowId, getFlowFiles(correlationSet), durations);
    }

    private List<Long[]> getDurations(List<Pair<Long, String>> correlationSet) {
        List<Long[]> longs = correlationSet.stream().map(pair -> {
            String filename = pair.getRight();
            String[] split = filename.split(Model.DELIM);
            return new Long[]{Long.parseLong(split[split.length - 4]), Long.parseLong(split[split.length - 3])};
        }).collect(Collectors.toList());
        return longs;
    }

    private List<String> getFlowFiles(List<Pair<Long, String>> correlationSet) {
        return correlationSet.stream().map(pair -> pair.getRight()).collect(Collectors.toList());
    }
}
