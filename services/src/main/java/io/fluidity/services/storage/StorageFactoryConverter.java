package io.fluidity.services.storage;

import io.fluidity.services.aws.AWS;
import io.fluidity.services.aws.AwsS3StorageService;
import io.fluidity.services.fixture.FixturedStorageService;
import io.fluidity.services.server.FileSystemBasedStorageService;
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

        String mode1 = System.getProperty("mode", "not-set");
        log.info(mode1);
        if (!mode1.equals("not-set")) {
            log.info("Overriding profile with system property:" + mode1);
            mode = mode1;
        }

        if (mode.equals("SERVER")) {
            storage = new FileSystemBasedStorageService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
            storage = new AwsS3StorageService();
        } else {
            storage = new FixturedStorageService();
        }
        return storage;
    }
}
