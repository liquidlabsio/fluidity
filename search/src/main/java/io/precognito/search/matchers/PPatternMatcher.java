package io.precognito.search.matchers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PPatternMatcher implements PMatcher {
    public static final String PREFIX = "record.matches(";
    private Pattern pattern;

    public PPatternMatcher(String expression) {
        String expr = expression.substring(PREFIX.length(), expression.length()-1);
        pattern = Pattern.compile(expr);
    }

    public PPatternMatcher() {
    }
    public boolean isForMe(String expression) {
        return expression.contains(".*");
    }

    @Override
    public boolean matches(String nextLine) {
        Matcher matcher = pattern.matcher(nextLine);
        return matcher.matches();
    }

    @Override
    public PMatcher clone(String expr) {
        return new PPatternMatcher(expr);
    }
}
