package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.StorageIndexer;
import org.eclipse.microprofile.config.spi.Converter;

public class FixturedIndexConverter implements Converter<StorageIndexer> {
    @Override
    public StorageIndexer convert(String s) {
        return new FixturedIndexerService();
    }
}
