package io.precognito.search.matchers;

public class GrepMatcher implements PMatcher {
    private final String expr;

    public GrepMatcher(String expr) {
        this.expr = expr;
    }

    public GrepMatcher() {
        this.expr = "NO!";
    }

    @Override
    public boolean isForMe(String expression) {
        return !expression.contains("*") && expression.length() > 0;
    }

    @Override
    public boolean matches(String nextLine) {
        return nextLine.indexOf(expr) > -1;
    }

    @Override
    public PMatcher clone(String expr) {
        return new GrepMatcher(expr);
    }
}
