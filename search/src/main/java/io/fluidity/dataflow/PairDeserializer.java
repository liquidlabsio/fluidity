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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.graalvm.collections.Pair;

import java.io.IOException;

public class PairDeserializer<K,V> extends StdDeserializer<Pair<K, V>> {
    private Class left;
    private Class right;
    ObjectMapper mapper = new ObjectMapper();

    public PairDeserializer(Class left, Class right) {
        this(null);
        this.left = left;
        this.right = right;
    }

    public PairDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Pair<K, V> deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        Object left = mapper.readValue(node.get("left").toString(), this.left);
        Object right = mapper.readValue(node.get("right").toString(), this.right);
        return (Pair<K, V>) Pair.create(left, right);
    }
}
