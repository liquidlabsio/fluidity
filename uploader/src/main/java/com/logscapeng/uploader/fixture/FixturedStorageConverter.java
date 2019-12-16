package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.StorageUploader;
import org.eclipse.microprofile.config.spi.Converter;

public class FixturedStorageConverter implements Converter<StorageUploader> {
    @Override
    public StorageUploader convert(String s) {
        return new FixturedUploaderService();
    }
}
