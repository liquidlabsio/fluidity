package com.logscapeng.uploader;

import org.eclipse.microprofile.config.spi.Converter;

public class FixturedStorageConvertor implements Converter<StorageUploader> {
    @Override
    public StorageUploader convert(String s) {
        return new FixturedUploaderService();
    }
}
