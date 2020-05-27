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

package io.fluidity.search.agg.events;

import io.fluidity.search.Search;

import java.io.IOException;

public interface EventCollector extends AutoCloseable {
    /**
     * Note: lines must be written to an .evt (event) outputstream using: timestamp:filepos:data
     */
    Integer[] process(boolean isCompressed, Search search, long fileFromTime, long fileToTime, long length, String timeFormat) throws IOException;
}
