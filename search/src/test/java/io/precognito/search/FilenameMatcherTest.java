package io.precognito.search;

import io.precognito.util.DateUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilenameMatcherTest {

    @Test
    void matchesStar() {
        FilenameMatcher filenameMatcher = new FilenameMatcher("*", 0, System.currentTimeMillis());
        assertTrue(filenameMatcher.matches("anything", System.currentTimeMillis()-1000, System.currentTimeMillis()));
    }

    @Test
    void matchesText() {
        FilenameMatcher filenameMatcher = new FilenameMatcher("filename.contains(myfile)", 0, System.currentTimeMillis());
        assertTrue(filenameMatcher.matches("this_is_myfile", System.currentTimeMillis()-1000, System.currentTimeMillis()));
    }

    @Test
    void matchesTimeWithin() {
        long to = System.currentTimeMillis();
        long from = to - DateUtil.HOUR;

        FilenameMatcher filenameMatcher = new FilenameMatcher("*", from, to);
        assertTrue(filenameMatcher.matches("before-overlap", from-DateUtil.MINUTE,  from+DateUtil.MINUTE));
        assertTrue(filenameMatcher.matches("after-overlap", to-DateUtil.MINUTE,  to+DateUtil.MINUTE));
        assertTrue(filenameMatcher.matches("within", from+DateUtil.MINUTE,  to-DateUtil.MINUTE));
        assertTrue(filenameMatcher.matches("biggerThan", from-DateUtil.MINUTE,  to+DateUtil.MINUTE));

        assertFalse(filenameMatcher.matches("before", from-DateUtil.HOUR,  from-DateUtil.MINUTE));
        assertFalse(filenameMatcher.matches("after", to+DateUtil.MINUTE,  to+DateUtil.HOUR));

    }

}