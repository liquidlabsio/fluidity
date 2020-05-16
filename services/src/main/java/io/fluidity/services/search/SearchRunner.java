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

package io.fluidity.services.search;

import io.fluidity.search.Search;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;

public interface SearchRunner {


    FileMeta[] submit(Search search, QueryService query);

    /**
     * @param files   - set of files to search
     * @param search
     * @param storage
     * @param region
     * @param tenant
     * @return
     */
    String[] searchFile(FileMeta files, Search search, Storage storage, String region, String tenant);

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
