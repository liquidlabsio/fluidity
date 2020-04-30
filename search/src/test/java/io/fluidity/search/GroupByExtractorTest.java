package io.fluidity.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupByExtractorTest {

    @Test
    void applyGroupByTag() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(tag)");
        String grouping = groupByExtractor.applyGrouping("tag", "sourcePath");
        assertEquals("tag", grouping);
    }
    @Test
    void applyGroupByPath() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path)");
        String grouping = groupByExtractor.applyGrouping("tag", "/some/source/path");
        assertEquals("/some/source/path", grouping);
    }

    @Test
    void applyGroupByPathWithSingleArg() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[2])");
        String grouping = groupByExtractor.applyGrouping("tag", "/some/source/path");
        assertEquals("source", grouping);
    }
    @Test
    void applyGroupByPathWithMultiArgs() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[1- 2])");
        String grouping = groupByExtractor.applyGrouping("tag", "/some/source/path");
        assertEquals("some-source", grouping);
    }
    @Test
    void applyGroupByPathWithLast() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[last])");
        String grouping = groupByExtractor.applyGrouping("tag", "/some/source/path");
        assertEquals("path", grouping);
    }

    @Test
    void itsAllBad() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[a1-])");
        String grouping = groupByExtractor.applyGrouping("tag", "/some/source/path");
        assertEquals("/some/source/path", grouping);
    }
}