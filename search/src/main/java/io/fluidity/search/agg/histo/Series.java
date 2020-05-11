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

    interface Ops<V> {
        V add(V t1, V t2);
    }

    class LongOps implements Ops<Long> {
        @Override
        public Long add(Long currentValue, Long newValue) {
            currentValue = currentValue == null ? 0 : currentValue;
            newValue = newValue == null ? 0 : newValue;
            return currentValue + newValue;

        }
    }
}
