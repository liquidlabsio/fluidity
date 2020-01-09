package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageIndexer;

public class FixturedIndexerService implements StorageIndexer {
    @Override
    public FileMeta enrichMeta(FileMeta fileMeta) {
        return null;
    }

    @Override
    public FileMeta index(FileMeta fileMeta, String cloudRegion) {
        return null;
    }
}
