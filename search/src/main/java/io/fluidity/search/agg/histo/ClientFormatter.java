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

import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientFormatter {
    /**
     * Convert to the client data model
     * {
     * * name: "Series 1",
     * * data: [
     * * [1486684800000, 34],
     * * [1486771200000, 43],
     * * [1486857600000, 31] ,
     * * [1486944000000, 43],
     * * [1487030400000, 33],
     * * [1487116800000, 52]
     * * ]
     * * }
     *
     * @param processSeries
     * @return
     */

    public static List<Map<String, Object>> getHistoSeriesForClient(List<Series<Long>> processSeries) {
        return processSeries.stream().map(series -> {
            Map<String, Object> seriesMap = new HashMap<>();
            seriesMap.put("name", series.name());
            seriesMap.put("data", convertToLongArray(series.data()));
            return seriesMap;
        }).collect(Collectors.toList());
    }

    private static Long[][] convertToLongArray(List<Pair<Long, Long>> data) {
        Long[][] results = new Long[data.size()][];
        for (int i = 0; i < data.size(); i++) {
            results[i] = new Long[2];
            results[i][0] = data.get(i).getLeft();
            results[i][1] = data.get(i).getRight();
        }
        return results;
    }
}
