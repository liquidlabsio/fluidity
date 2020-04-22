package io.fluidity.search.field.matchers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanMatcher implements PMatcher {
    public static final String PREFIX_1 = " AND ";
    public static final String PREFIX_2 = " OR ";
    private List<List<String>> expr;

    public BooleanMatcher(){
    }
    public BooleanMatcher(String expression) {
        String[] ands = expression.split(" AND ");
        expr = Arrays.stream(ands).map(and -> Arrays.asList(and.split(" OR "))).collect(Collectors.toList());
    }

    @Override
    public boolean isForMe(String expression) {
        return expression.contains(PREFIX_1) || expression.contains(PREFIX_2);
    }

    @Override
    public boolean matches(String nextLine) {
        long andCount = expr.stream().filter(exprPart -> exprPart.stream().filter(orPart -> nextLine.contains(orPart)).count() > 0).count();
        return andCount == expr.size();
    }

    @Override
    public PMatcher clone(String expr) {
        return new BooleanMatcher(expr);
    }
}
