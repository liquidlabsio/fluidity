package io.fluidity.search;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.*;

class GroupByExtractorTest {

    @Test
    void applyGroupByTag() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(tags)");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "sourcePath");
        assertEquals("tag-key", stringObjectSimpleEntry.getKey());;
    }
    @Test
    void applyGroupByPath() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path)");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "/some/source/path");
        assertEquals("/some/source/path-key", stringObjectSimpleEntry.getKey());;
    }

    @Test
    void applyGroupByPathWithSingleArg() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[2])");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "/some/source/path");
        assertEquals("source-key", stringObjectSimpleEntry.getKey());;
    }
    @Test
    void applyGroupByPathWithMultiArgs() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[1- 2])");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "/some/source/path");
        assertEquals("some-source-key", stringObjectSimpleEntry.getKey());;
    }
    @Test
    void applyGroupByPathWithLast() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[last])");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "/some/source/path");
        assertEquals("path-key", stringObjectSimpleEntry.getKey());;
    }

    @Test
    void itsAllBad() {
        GroupByExtractor groupByExtractor = new GroupByExtractor("bucket| filename| record| field| analytic| timeseries| groupBy(path[a1-])");
        AbstractMap.SimpleEntry<String, Object> pair = new AbstractMap.SimpleEntry<>("key", "value");
        AbstractMap.SimpleEntry<String, Object> stringObjectSimpleEntry = groupByExtractor.applyGrouping(pair, "tag", "/some/source/path");
        assertEquals("/some/source/path-key", stringObjectSimpleEntry.getKey());;
    }
}