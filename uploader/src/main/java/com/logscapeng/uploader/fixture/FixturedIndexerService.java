package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageIndexer;

public class FixturedIndexerService implements StorageIndexer {
    @Override
    public FileMeta enrichMeta(FileMeta fileMeta) {
        return fileMeta;
    }

    @Override
    public FileMeta index(FileMeta fileMeta, String cloudRegion) {
        if (fileMeta.getToTime() == 0) {
            fileMeta.setFromTime(System.currentTimeMillis() - 10000);
            fileMeta.setToTime(System.currentTimeMillis());
        }

        if (fileMeta.getFromTime() == 0) {
            fileMeta.setFromTime(fileMeta.getToTime() - (15 * 60 * 1000));
        }
        return fileMeta;
    }
}
