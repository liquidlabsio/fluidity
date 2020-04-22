package io.fluidity.search.field.matchers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class RecordMatcherFactory {
    /**
     * RecordMatchers [record.contains(XXX), record.matches(.*), or * for everything
     * @param expression
     * @return
     */
    public static List<PMatcher> matchers = Arrays.asList(new BooleanMatcher(), new PPatternMatcher(), new GrepMatcher(), new AllMatcher());

    public static PMatcher getMatcher(String recordFilter) {
        List<PMatcher> collect = matchers.stream().filter(matcher -> matcher.isForMe(recordFilter)).collect(Collectors.toList());
        if (collect.size() == 0) {
            return recordFilter.equals("*") ? new AllMatcher() : new GrepMatcher(recordFilter);
        } else return collect.iterator().next().clone(recordFilter);
    }
}
