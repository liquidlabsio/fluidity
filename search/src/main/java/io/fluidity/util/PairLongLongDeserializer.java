/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed
 *   on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.graalvm.collections.Pair;

import java.io.IOException;

public class PairLongLongDeserializer extends StdDeserializer<Pair<Long, Long>> {

    public PairLongLongDeserializer() {
        this(null);
    }

    public PairLongLongDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public Pair<Long, Long> deserialize(final JsonParser jp, final DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jp.getCodec().readTree(jp);
        final Long left = node.get("left").longValue();
        final Long right = node.get("right").longValue();
        return Pair.create(left, right);
    }
}
