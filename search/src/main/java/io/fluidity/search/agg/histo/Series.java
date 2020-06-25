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

package io.fluidity.search.agg.histo;

import org.graalvm.collections.Pair;

import java.util.Collection;
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

    Collection<Series<T>> slice(long timeBucket);

    long start();

    long end();

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
