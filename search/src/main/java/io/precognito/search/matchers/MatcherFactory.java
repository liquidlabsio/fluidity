package io.precognito.search.matchers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class MatcherFactory {
    /**
     * TODO: parse SQL or Piped expression to determine line matching function
     * @param expression
     * @return
     */
    public static List<PMatcher> matchers = Arrays.asList(new PPatternMatcher(), new GrepMatcher(), new AllMatcher());

    public static PMatcher getMatcher(String expression) {
        String[] split = expression.split("\\|");

        final String lineMatcherExpression = split.length > 4 ? split[4].trim() : "";


        List<PMatcher> collect = matchers.stream().filter(matcher -> matcher.isForMe(lineMatcherExpression)).collect(Collectors.toList());
        if (collect.size() == 0) return new AllMatcher();
        else return collect.iterator().next().clone(lineMatcherExpression);
    }
}
