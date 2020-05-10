package io.fluidity.search.agg.histo;

import org.graalvm.collections.Pair;

import java.util.List;

public interface Series<T> {
    String groupBy();

    T get(long time);

    void update(long time, T value);

    int index(long time);

    boolean hasData();

    List<Pair<Long, T>> data();

    String name();

    void merge(Series<T> series);

}
