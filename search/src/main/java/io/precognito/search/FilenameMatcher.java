package io.precognito.search;

public class FilenameMatcher {
    private final String expressionPart;
    private final long searchFrom;
    private final long searchTo;

    public FilenameMatcher(String expression, long searchFrom, long to) {
        this.searchFrom = searchFrom;
        this.searchTo = to;
        String[] split = expression.split("\\|");
        expressionPart = split.length > Search.EXPRESSION_PARTS.filename.ordinal() ? split[Search.EXPRESSION_PARTS.filename.ordinal()].trim() : "";
    }

    public boolean matches(String filename, long from, long to) {
        if (from < searchFrom && to > searchFrom
                || from < searchTo && to > searchTo
                || from > searchFrom && from < searchTo
                || to > searchFrom && to < searchTo
        ) {
            return filename.contains(expressionPart);
        }
        return false;
    }
}
