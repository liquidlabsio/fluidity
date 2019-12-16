package com.logscapeng.uploader.aws;

import com.logscapeng.uploader.StorageUploader;
import org.eclipse.microprofile.config.spi.Converter;

public class AwsStorageConvertor implements Converter<StorageUploader> {
    @Override
    public StorageUploader convert(String s) {
        return new AwsS3StorageUploaderService();
    }
}
