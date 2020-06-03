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

package io.fluidity.search.field.extractor;

import org.graalvm.collections.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KvPairExtractorTest {

    @Test
    void getKeyAndValue() {

        int i = 523 -  (523 % 100);


        KvPairExtractor extractor = new KvPairExtractor("RequestId: ");
        String nextLine = "2020-06-03T08:33.18.117Z REPORT RequestId: f947b07c-f755-4e94-bc72-418916c489a6 Duration: 881 ms        Billed Duration: 1000 ms";

        Pair<String, Long> source = extractor.getKeyAndValue("source", nextLine);
        assertEquals("f947b07c-f755-4e94-bc72-418916c489a6", source.getLeft());
    }
}