package io.fluidity.search.field.extractor;

import org.graalvm.collections.Pair;

/**
 * Support extraction of:
 * ..."user":"99kerob".... with field.getJsonKVPair(user)
 * ..."minor":12123 ,.... with field.getJsonKVPair(user)
 */
public class KvJsonPairExtractor implements Extractor {

    private final String token;

    public KvJsonPairExtractor(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Pair<String, Long> getKeyAndValue(String sourceName, String nextLine) {
        if (token.contains(".")) {
            String[] split = token.split("\\.");
            Pair<String, Long> value1 = getValue(split[0], nextLine);
            Pair<String, Long> value2 = getValue(split[1], nextLine);
            return Pair.create(value1.getLeft() + "." + value2.getLeft(), 1L);
        } else {
            String key = token;
            Pair<String, Long> result = getValue(key, nextLine);
            return result;
        }
    }

    private Pair<String, Long> getValue(String key, String nextLine) {

        int foundIndex = nextLine.indexOf("\"" + key + "\"");
        int delimiter = nextLine.indexOf(":", foundIndex);
        if (foundIndex >= 0 && delimiter > 0) {
            int nextFieldDelimiter = nextLine.indexOf(",", delimiter);
            if (nextFieldDelimiter == -1) {
                nextFieldDelimiter = nextLine.indexOf("\"", delimiter);
            }
            if (nextFieldDelimiter == -1) {
                nextFieldDelimiter = nextLine.indexOf("}", delimiter);
            }
            int nextStringMarker = nextLine.indexOf("\"", delimiter);
            // numeric mode - when the \" is after the next delimiter - it means we have "someField":1234, "anotherField":
            if (nextFieldDelimiter < nextStringMarker) {
                // numeric mode - token:value, OR  token: value , (missing quotations)
                // numeric mode uses token:value pairs.
                String valueString = nextLine.substring(delimiter + 1, nextFieldDelimiter);
                String cleanValue = valueString.trim();
                if (cleanValue.contains(".")) {
                    return Pair.create(key, Double.valueOf(cleanValue).longValue());
                } else {
                    return Pair.create(key, Long.valueOf(cleanValue));
                }

            } else {
                // string mode - is different to numeric mode - the value is used in the KV - i.e. count how many users
                int toIndex = nextLine.indexOf("\"", nextStringMarker + 1);
                String value = nextLine.substring(nextStringMarker + 1, toIndex);
                //return Pair.create(token, value);
                // TODO: not convinced
                return Pair.create(value, 1l);
            }
        }
        return null;

    }
}

