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

/**
 * Flips the server side ladder format to client side.
 * i.e.
 * {
 *   "groupBy" : "",
 *   "name" : "none",
 *   "data" : [ {
 *     "left" : 1591271280000, "right" : null }, { "left" : 1591271460000, "right" : null }, {
 *     "left" : 1591271640000,
 *     "right" : {
 *       "47100" : {
 *         "opLatency" : [ -43200, 0, 0 ],
 *         "opDuration" : [ 47143, 47143, 141429 ],
 *         "duration" : [ 47143, 47143, 141429 ],
 *         "count" : 3
 *       }
 *     }
 *   }, <etc>
 *
 *  TO
 *   timestamp:   [ t, t+1, t+1, t+3 ... ]
 *   latency-1:   [ 100, 200, 300, 400...]
 *   count-1:     [ 10, 25, 30, 10   ... ]
 *   latency-2:   [ 100, 250, 300, 100...]
 *   count-2:     [ 10, 25, 30, 10....]
 *   latency-3:   [ 100, 250, 300, 100...]
 *   count-3:     [ 10, 25, 30, 10....]
 *
 *
 *
 *
 *   Note: an ac
 */
public class ClientLadderJsonConvertor {
}
