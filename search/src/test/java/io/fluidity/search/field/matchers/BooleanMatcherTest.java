package io.fluidity.search.field.matchers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BooleanMatcherTest {

    @Test
    void isForMe() {

        assertTrue(new BooleanMatcher("").isForMe("Helloa AND hi"));
        assertTrue(new BooleanMatcher("").isForMe("Helloa OR hi"));
        assertTrue(new BooleanMatcher("").isForMe("Helloa AND yes OR NO"));
    }

    @Test
    void matches() {

        assertTrue(new BooleanMatcher("Hello").matches("this is Hello"));
        assertTrue(new BooleanMatcher("hello AND goodbye").matches("this is hello and this is goodbye"));
        assertTrue(new BooleanMatcher("hello OR maybe").matches("this is hello and this is goodbye"));
        assertTrue(new BooleanMatcher("hello OR maybe").matches("this is maybe"));

        assertFalse(new BooleanMatcher("hello AND maybe").matches("this is maybe"));
        assertTrue(new BooleanMatcher("hello OR maybe").matches("this is maybe"));


    }
}