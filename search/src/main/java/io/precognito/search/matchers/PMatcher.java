package io.precognito.search.matchers;

public interface PMatcher {
    boolean isForMe(String expression);
    boolean matches(String nextLine);
    PMatcher clone(String expr);
}
