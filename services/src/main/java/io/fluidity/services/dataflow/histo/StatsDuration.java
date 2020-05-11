package io.fluidity.services.dataflow.histo;

public class StatsDuration {
    long minTotalDuration = -1;
    long sumTotalDuration = 0;
    long maxTotalDuration = -1;

    public void update(long value) {
        if (minTotalDuration == -1 || minTotalDuration < value) minTotalDuration = value;
        if (maxTotalDuration == -1 || maxTotalDuration > value) maxTotalDuration = value;
        sumTotalDuration += value;
    }
}
