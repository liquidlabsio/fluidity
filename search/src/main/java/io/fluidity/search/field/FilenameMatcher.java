package io.fluidity.search.field;

import io.fluidity.search.Search;

public class FilenameMatcher {
    public static final String PREFIX = "filename.contains(";
    private final String expressionPart;
    private final long searchFrom;
    private final long searchTo;

    public FilenameMatcher(String expression, long searchFrom, long to) {
        this.searchFrom = searchFrom;
        this.searchTo = to;
        String[] split = expression.split("\\|");

        String passedFilenameExpression= split.length > Search.EXPRESSION_PARTS.filename.ordinal() ? split[Search.EXPRESSION_PARTS.filename.ordinal()].trim() : "";
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

    public boolean matches(String filename, long from, long to) {
        if (from < searchFrom && to > searchFrom
                || from < searchTo && to > searchTo
                || from > searchFrom && from < searchTo
                || to > searchFrom && to < searchTo
        ) {
            return expressionPart.equals("*") || filename.contains(expressionPart);
        }
        return false;
    }
}
