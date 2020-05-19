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

package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;

import java.util.List;

public interface SearchRunner {


    FileMeta[] submit(String tenant, Search search, QueryService query);

    /**
     * @param files   - set of files to search
     * @param search
     * @param storage
     * @param region
     * @param tenant
     * @return
     */
    List<Integer[]> searchFile(FileMeta[] files, Search search, Storage storage, String region, String tenant);

    String finalizeHisto(Search search, String tenant, String region, Storage storage);

    /**
     * Returns [ numEvents, Histo, rawEvents ]
     *
     * @param search
     * @param tenant
     * @param region
     * @param storage
     * @return
     */
    String[] finalizeEvents(Search search, long from, int limit, String tenant, String region, Storage storage);
}
