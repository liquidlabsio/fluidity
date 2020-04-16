package io.fluidity.search.agg.histo;

import java.util.List;

public interface Series {
    long get(long time);

    void update(long time, long value);

    int index(long time);

    boolean hasData();

    List<Long[]> data();

    String name();
}
