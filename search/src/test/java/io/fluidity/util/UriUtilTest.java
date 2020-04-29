package io.fluidity.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UriUtilTest {

    @Test
    public void getWindowsHostnameAndPath() {
        String example = "D:\\work\\logs\\edit-stream-2020-04-27-0943.log.lz4";
        String[] hostnameAndPath = UriUtil.getHostnameAndPath(example);
        assertEquals("work", hostnameAndPath[0]);
    }
}