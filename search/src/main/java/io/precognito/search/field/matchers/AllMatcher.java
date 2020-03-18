package io.precognito.search.field.matchers;

public class AllMatcher implements PMatcher {
    @Override
    public boolean isForMe(String expression) {
        return false;
    }

    @Override
    public boolean matches(String nextLine) {
        return true;
    }

    @Override
    public PMatcher clone(String expr) {
        return this;
    }
}
