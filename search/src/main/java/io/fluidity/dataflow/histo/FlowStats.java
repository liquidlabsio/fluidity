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

package io.fluidity.dataflow.histo;

import io.fluidity.dataflow.FlowInfo;

import java.util.ArrayList;
import java.util.Arrays;

public class FlowStats {
    // min,max,sum
    long[] opLatency = new long[]{Long.MAX_VALUE, 0, 0};

    // min, max, sum
    long[] opDuration = new long[]{Long.MAX_VALUE, 0, 0};

    // min, max, sum
    long[] duration = new long[] {Long.MAX_VALUE, 0, 0};

    long count = 0;

    public void update(FlowInfo flow) {
        long[] minOp2OpLatency = flow.getMinMaxOp2OpLatencyWithMaxOpAndE2E();
        setMinMax(minOp2OpLatency[0], opLatency, false);
        setMinMax(minOp2OpLatency[1], opLatency, true);
        setMinMax(minOp2OpLatency[2], opDuration, true);
        setMinMax(minOp2OpLatency[3], duration, true);
        count++;
    }

    private void setMinMax(long value, long[] data, boolean includeSum) {
        data[0] = Math.min(value, data[0]);
        data[1] = Math.max(value, data[0]);
        if (includeSum) {
            data[2] += value;
        }
    }

    public Long[] data() {
        ArrayList<Long> results = new ArrayList<>();
        results.add(count);
        Arrays.stream(opLatency).forEach(item -> results.add(item));
        Arrays.stream(opDuration).forEach(item -> results.add(item));
        Arrays.stream(duration).forEach(item -> results.add(item));
        return results.toArray(new Long[results.size()]);
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long[] getOpLatency() {
        return opLatency;
    }

    public void setOpLatency(long[] opLatency) {
        this.opLatency = opLatency;
    }

    public long[] getOpDuration() {
        return opDuration;
    }

    public void setOpDuration(long[] opDuration) {
        this.opDuration = opDuration;
    }

    public long[] getDuration() {
        return duration;
    }

    public void setDuration(long[] duration) {
        this.duration = duration;
    }
}
