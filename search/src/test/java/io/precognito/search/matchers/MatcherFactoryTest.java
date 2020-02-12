package io.precognito.search.matchers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatcherFactoryTest {

    @Test
    public void testGrepReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "CPU";
        PMatcher matcher = MatcherFactory.getMatcher(expression);
        assertEquals(GrepMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());
    }

    @Test
    public void testPatternReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = ".*CPU.*";
        PMatcher matcher = MatcherFactory.getMatcher(expression);
        assertEquals(PPatternMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());
    }

    @Test
    public void testAllMatchReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "*";
        PMatcher matcher = MatcherFactory.getMatcher(expression);
        assertEquals(AllMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());
    }



}