/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.search.agg.histo;

import io.fluidity.search.Search;

import java.io.InputStream;
import java.util.Map;

/**
 * Returns timeseries histogram data that looks like this:
 *  series: [
 *     {
 *       name: "Series 1",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     },
 *      {
 *       name: "Series 2",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     }
 *   ]
 */
public interface HistoAggregator<T> extends AutoCloseable {
    String process() throws Exception;

    boolean isForMe(String analytic);

    HistoAggregator<T> clone(Map<String, InputStream> inputStreams, Search search);

    HistoFunction function();
}
