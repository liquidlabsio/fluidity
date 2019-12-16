package com.logscapeng.uploader.aws;

import com.logscapeng.uploader.StorageUploader;
import com.logscapeng.uploader.fixture.FixturedUploaderService;
import org.eclipse.microprofile.config.spi.Converter;

public class AwsStorageConverter implements Converter<StorageUploader> {
    @Override
    public StorageUploader convert(String s) {
        if (s.equals("TEST")) {
            return new FixturedUploaderService();
        }
        return new AwsS3StorageUploaderService();
    }
}
