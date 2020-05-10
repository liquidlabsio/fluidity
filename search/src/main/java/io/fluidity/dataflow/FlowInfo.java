package io.fluidity.dataflow;

import java.util.List;

public class FlowInfo {
    /**
     * Info to render the span breakdown
     */
    public String flowId;
    public List<String> flowFiles;
    public long durationMs;
    public List<Long[]> durations;

    public FlowInfo() {
    }

    public FlowInfo(String flowId, List<String> flowFiles, List<Long[]> durations) {
        this.flowId = flowId;
        this.durations = durations;
        this.durationMs = getEnd() - getStart();
        this.flowFiles = flowFiles;
    }

    public long getStart() {
        return durations.get(0)[0];
    }

    public long getEnd() {
        return durations.get(durations.size() - 1)[1];
    }

    public long getDuration() {
        return getEnd() - getStart();
    }

    public long[] getMinOp2OpLatency() {
        long min = Long.MAX_VALUE;
        long max = 0;
        long maxOpDurtion = Long.MAX_VALUE;
        for (int i = 0; i < durations.size(); i++) {
            if (i > 0) {
                long interval = durations.get(i)[0] - durations.get(i - 1)[1];
                if (interval < min) min = interval;
                if (interval > max) max = interval;
            }
            long duration = durations.get(i)[1] - durations.get(i)[0];
            if (maxOpDurtion < duration) maxOpDurtion = duration;
        }
        return new long[]{min, max, maxOpDurtion};
    }

}
