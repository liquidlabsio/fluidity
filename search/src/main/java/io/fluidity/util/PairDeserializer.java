package io.fluidity.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.graalvm.collections.Pair;

import java.io.IOException;

public class PairDeserializer extends StdDeserializer<Pair> {

    public PairDeserializer() {
        this(null);
    }

    public PairDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Pair deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        Long left = node.get("left").longValue();
        Long right = node.get("right").longValue();
        return Pair.create(left, right);
    }
}
