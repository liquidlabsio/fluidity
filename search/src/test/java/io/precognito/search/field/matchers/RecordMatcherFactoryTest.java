package io.precognito.search.field.matchers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordMatcherFactoryTest {

    @Test
    public void testGrepReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "record.contains(CPU)";
        PMatcher matcher = RecordMatcherFactory.getMatcher(expression);
        assertEquals(GrepMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());

        assertTrue(matcher.matches("some line of CPU data"));
        assertFalse(matcher.matches("some line of C_PU data"));
    }

    @Test
    public void testPatternReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "record.matches(.*CPU.*)";
        PMatcher matcher = RecordMatcherFactory.getMatcher(expression);
        assertEquals(PPatternMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());

        assertTrue(matcher.matches("some line of CPU data"));
        assertFalse(matcher.matches("some line of C_PU data"));

    }

    @Test
    public void testAllMatchReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "*";
        PMatcher matcher = RecordMatcherFactory.getMatcher(expression);
        assertEquals(AllMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());
    }

    @Test
    public void testLazyMatcherReturned() throws Exception {
        // bucket | host | tags | filename | lineMatcher | fieldExtractor
        String expression = "error";
        PMatcher matcher = RecordMatcherFactory.getMatcher(expression);
        assertEquals(GrepMatcher.class.getSimpleName(), matcher.getClass().getSimpleName());
    }


}