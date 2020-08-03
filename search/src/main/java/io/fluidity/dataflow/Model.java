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

package io.fluidity.dataflow;

public class Model {

    public final static Long LADDER_GRANULARITY = Long.getLong("ladder.granularity", 50l);
    public final static String DELIM = "_";
    /**
     * The correlation file contains an extracted set of line items that contain a common-correlation id
     */
    public final static String CORR_FILE_FMT = "%s/corr_%d_%d_%s_.corr";
    public final static String CORR_PREFIX = "/corr_";

    /**
     * DAT files contain the meta data model for a correlation including:
     * service, operation, type, meta, tag, behavior
     * These attributes are used to enrich the search functionality when analysing correlations
     */
    public final static String CORR_DAT_FMT = "%s/dat_%d_%d_%s_.dat";
    public final static String CORR_DAT_PREFIX = "/dat_";

    /**
     * The set of related correlation files as well as flow level stats (durations, latencies etc)
     */
    public final static String CORR_FLOW_FMT_2 = "%s/flow_%d_%d_%s_.flow";
    public final static String CORR_FLOW_PREFIX = "/flow_";

    /**
     * The 20k foot ladder histogram that maps flows into ladders/buckets
     */
    public final static String LADDER_HIST_FMT = "%s/ladder_%d_%d_.ladder";
    public final static String LADDER_HIST_PREFIX = "/ladder_";

    /**
     * The 50k view of flows. Includes volume, throughput and latency aggregates
     */
    public final static String CORR_HIST_FMT = "%s/histo_%d_%d_.histo";
    public final static String CORR_HIST_PREFIX = "/histo_";


    /**
     * Get offsets from split names
     */
    public static final int FROM_END_INDEX = 3;
    public static final int TO_END_INDEX = 2;
}
