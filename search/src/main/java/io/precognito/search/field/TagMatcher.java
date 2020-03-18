package io.precognito.search.field;

import io.precognito.search.Search;

public class TagMatcher {
    public static final String PREFIX = "tags.contains(";
    private final String expressionPart;

    public TagMatcher(String expression) {
        String[] split = expression.split("\\|");

        String passedFilenameExpression = split.length > Search.EXPRESSION_PARTS.bucket.ordinal() ? split[Search.EXPRESSION_PARTS.bucket.ordinal()].trim() : "";
        if (passedFilenameExpression.startsWith(PREFIX)) {
            int startsFrom = expression.indexOf(PREFIX);
            int endsAt = expression.indexOf(")", startsFrom);
            this.expressionPart = expression.substring(startsFrom + PREFIX.length(), endsAt);
        } else if (passedFilenameExpression.equals("*")) {
            this.expressionPart = "*";
        } else {
            this.expressionPart = "*";
        }

    }

    public boolean matches(String tags) {
        return expressionPart.equals("*") || tags.contains(expressionPart);
    }
}
