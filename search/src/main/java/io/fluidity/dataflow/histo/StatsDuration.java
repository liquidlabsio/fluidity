package io.fluidity.dataflow.histo;

public class StatsDuration {
    long minTotalDuration = -1;
    long sumTotalDuration = 0;
    long maxTotalDuration = -1;
    long count = 0;

    public void update(long value) {
        if (minTotalDuration == -1 || value < minTotalDuration) minTotalDuration = value;
        if (maxTotalDuration == -1 || value > maxTotalDuration) maxTotalDuration = value;
        sumTotalDuration += value;
        count++;
    }

    public Long[] data() {
        return new Long[]{minTotalDuration, maxTotalDuration, sumTotalDuration, count};
    }
}
