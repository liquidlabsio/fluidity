package io.fluidity.search.field.extractor;

import io.fluidity.search.Search;
import org.graalvm.collections.Pair;

/**
 * Support fieldname and value extraction:
 * field.getKVPair("CPU:") - gets the value
 * field.getJsonPair("CPU:") - gets the value
 */
public class FieldExtractor {
    public static final String PREFIX = "field.";
    private final String expressionPart;
    private final String typeOf;
    private final Extractor extractor;


    public FieldExtractor(String expression) {
        String[] split = expression.split("\\|");
        String passedFilenameExpression = split.length > Search.EXPRESSION_PARTS.field.ordinal() ? split[Search.EXPRESSION_PARTS.field.ordinal()].trim() : "";
        if (passedFilenameExpression.startsWith(PREFIX)) {
            int startsFrom = expression.indexOf(PREFIX);
            int endsAt = expression.indexOf(")", startsFrom);
            int firstBracket = expression.indexOf("(", startsFrom);
            this.typeOf = expression.substring(startsFrom + PREFIX.length(), firstBracket);
            this.expressionPart = expression.substring(firstBracket + 1, endsAt);

            if (typeOf.equals("getKVPair")) {
                this.extractor = new KvPairExtractor(expressionPart);
            } else if (typeOf.equals("getJsonPair")) {
                this.extractor = new KvJsonPairExtractor(expressionPart);
            } else {
                this.extractor = null;
            }
        } else if (passedFilenameExpression.equals("*")) {
            this.expressionPart = "*";
            this.typeOf = "*";
            this.extractor = null;
        } else {
            this.expressionPart = "*";
            this.typeOf = "*";
            this.extractor = null;
        }
    }

    public Pair<String, Long> getSeriesNameAndValue(String sourceName, String nextLine) {
        if (expressionPart.equals("*")) return Pair.create(sourceName, 1l);
        return extractor.getKeyAndValue(sourceName, nextLine);
    }
}
