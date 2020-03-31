package io.precognito.services.storage;

import io.precognito.services.aws.AWS;
import io.precognito.services.aws.AwsS3StorageService;
import io.precognito.services.fixture.FixturedStorageService;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageFactoryConverter implements Converter<Storage> {
    private final Logger log = LoggerFactory.getLogger(StorageFactoryConverter.class);
    @Override
    public Storage convert(String mode) {
        log.info("Mode:" + mode);
        if (mode.equalsIgnoreCase(LaunchMode.TEST.name()) || mode.equals("SERVER")) {
            return new FixturedStorageService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
            return new AwsS3StorageService();
        }
        return new FixturedStorageService();
    }
}
