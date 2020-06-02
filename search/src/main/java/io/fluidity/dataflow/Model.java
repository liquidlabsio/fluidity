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

    public final static String DELIM = "_";
    // correlation-start-end
    public final static String CORR_FILE_FMT = "%s/corr_%s_%d_%d_.log";
    public final static String CORR_PREFIX = "/corr_";

    public final static String CORR_DAT_FMT = "%s/dat_%s_%d_%d_.dat";
    public final static String CORR_DAT_PREFIX = "/dat_";

    public final static String CORR_FLOW_FMT = "%s/flow_%s_%d_%d_.dat";
    public final static String CORR_FLOW_PREFIX = "/flow_";

    public final static String CORR_HIST_FMT = "%s/histo_%d_%d_.histo";
    public final static String CORR_HIST_PREFIX = "/histo_";

    public final static String LADDER_HIST_FMT = "%s/ladder_%d_%d_.ladder";
    public final static String LADDER_HIST_PREFIX = "/ladder_";

}
