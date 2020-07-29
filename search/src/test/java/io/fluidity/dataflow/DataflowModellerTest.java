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

import io.fluidity.util.DateUtil;
import org.graalvm.collections.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.fluidity.dataflow.Model.CORR_FILE_FMT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataflowModellerTest {

    @Test
    void getCorrelationFlow() {

        List<Pair<Long, String>> correlationSet = new ArrayList<>();
        long start = System.currentTimeMillis() - DateUtil.HOUR;
        long middle = start + DateUtil.MINUTE * 2;
        long end = start + DateUtil.MINUTE * 5;

        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", start, start + DateUtil.MINUTE, "txn123")));
        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", middle, middle + DateUtil.MINUTE, "txn123")));
        correlationSet.add(Pair.create(end, String.format(CORR_FILE_FMT, "/path", end, end + DateUtil.MINUTE, "txn123")));

        FlowInfo correlationFlow = new DataflowModeller().getCorrelationFlow("txn123", correlationSet);
        assertNotNull(correlationFlow);
        assertEquals(6, correlationFlow.durationMs / DateUtil.MINUTE);
        assertEquals(3, correlationFlow.durations.size());
        assertEquals(3, correlationFlow.flowFiles.size());
    }

    @Test
    void getCorrelationFlowStart() {
    }

    @Test
    void getCorrelationFlowEnd() {
    }
}