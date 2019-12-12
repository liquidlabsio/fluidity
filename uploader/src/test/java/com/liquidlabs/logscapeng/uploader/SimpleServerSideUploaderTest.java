package com.liquidlabs.logscapeng.uploader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleServerSideUploaderTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void upload() {
        SimpleServerSideUploader uploader = new SimpleServerSideUploader();

        String result = uploader.upload("tenant", "resource", "filename", "meta-json");
    }
}