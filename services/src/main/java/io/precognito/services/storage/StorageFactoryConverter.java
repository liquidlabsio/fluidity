package io.precognito.services.storage;

import io.precognito.services.aws.AWS;
import io.precognito.services.aws.AwsS3StorageService;
import io.precognito.services.fixture.FileSystemBasedStorageService;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageFactoryConverter implements Converter<Storage> {
    private final Logger log = LoggerFactory.getLogger(StorageFactoryConverter.class);

    private static Storage storage;

    @Override
    public Storage convert(String mode) {
        log.info("Mode:" + mode);

        if (storage != null) return storage;

        if (mode.equalsIgnoreCase(LaunchMode.TEST.name()) || mode.equals("SERVER")) {
            storage = new FileSystemBasedStorageService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
            storage = new AwsS3StorageService();
        } else {
            storage = new FileSystemBasedStorageService();
        }
        return storage;
    }
}
