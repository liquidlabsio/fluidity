package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMetaDataQueryService;
import org.eclipse.microprofile.config.spi.Converter;

public class FixturedQueryConverter implements Converter<FileMetaDataQueryService> {
    @Override
    public FileMetaDataQueryService convert(String s) {
        return new FixturedFileMetaDataQueryService();
    }
}
