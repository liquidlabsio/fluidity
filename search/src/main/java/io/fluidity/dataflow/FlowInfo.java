/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.dataflow;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FlowInfo {
    public static final int START_TIME_INDEX = 0;
    public static final int END_TIME_INDEX = 1;
    /**
     * Info to render the span breakdown
     */
    public String flowId;
    public List<String> flowFiles;
    public long durationMs;
    public List<Long[]> durations;
    public List<String> times;

    public FlowInfo() {
    }

    public FlowInfo(final String flowId, final List<String> flowFiles, final List<Long[]> durations) {
        this.flowId = flowId;
        this.durations = durations;
        this.durationMs = getEnd() - getStart();
        this.flowFiles = flowFiles;
        this.times =
                durations.stream().flatMap(ddd -> Arrays.stream(ddd).map(dd -> new Date(dd).toString())).collect(Collectors.toList());
    }

    @JsonIgnore
    public long getStart() {
        return durations.get(0)[0];
    }

    @JsonIgnore
    public long getEnd() {
        return durations.get(durations.size() - 1)[1];
    }

    @JsonIgnore
    public long getDuration() {
        return getEnd() - getStart();
    }

    @JsonIgnore
    public long[] getMinMaxOp2OpLatencyWithMaxOpAndE2E() {
        long minLatency = Long.MAX_VALUE;
        long maxLatency = 1;
        long maxOpDuration = 1;
        for (int i = 0; i < durations.size(); i++) {
            if (i > 0) {
                final long interval = durations.get(i)[START_TIME_INDEX] - durations.get(i - 1)[END_TIME_INDEX];
                if (interval < minLatency || interval == Long.MAX_VALUE) {
                    minLatency = interval;
                }
                if (interval > maxLatency) {
                    maxLatency = interval;
                }
            }
            final long duration = durations.get(i)[1] - durations.get(i)[0];
            if (maxOpDuration < duration && duration > 0) {
                maxOpDuration = duration;
            }
        }
        if (durations.size() == 1) {
            minLatency = 1;
        }
        if (durationMs == 0) {
            durationMs = 2;
        }
        return new long[]{minLatency, maxLatency, maxOpDuration, durationMs};
    }

    public List<Long> getDurationsAsInterval() {
        final List<Long> results = new ArrayList<>();
        for (Long[] times : this.durations) {
            results.add(times[1] - times[0]);
        }
        return results;
    }

    /**
     * Converts to UI flow format:
     * ['txn-1000', 1000, 400, 200,1000, 400],
     * @return
     */
    public List<Object> durations() {
        final List list = new ArrayList();
        list.add(flowId);
         durations.stream().forEach(duration -> list.add(duration[1] - duration[0]));
        return list;
    }
}
