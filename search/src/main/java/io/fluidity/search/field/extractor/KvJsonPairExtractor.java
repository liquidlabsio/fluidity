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
    public Pair<String, Object> getKeyAndValue(String sourceName, String nextLine) {
        int foundIndex = nextLine.indexOf("\"" + token + "\"");
        int delimiter = nextLine.indexOf(":", foundIndex);
        if (foundIndex >= 0 && delimiter > 0) {
            int nextFieldDelimiter  = nextLine.indexOf(",", delimiter);
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
                String valueString = nextLine.substring(delimiter+1, nextFieldDelimiter);
                String cleanValue = valueString.trim();
                if (cleanValue.contains(".")) {
                    return Pair.create(token, Double.valueOf(cleanValue));
                } else {
                    return Pair.create(token, Long.valueOf(cleanValue));
                }

            } else {
                // string mode - is different to numeric mode - the value is used in the KV - i.e. count how many users
                int toIndex = nextLine.indexOf("\"", nextStringMarker+1);
                String value = nextLine.substring(nextStringMarker+1, toIndex);
                return Pair.create(token, value);
            }

        } else {
            return null;
        }
    }
}
