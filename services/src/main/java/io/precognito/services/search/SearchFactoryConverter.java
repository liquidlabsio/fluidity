package io.precognito.services.search;

import io.precognito.services.aws.AWS;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchFactoryConverter implements Converter<SearchService> {
    private final Logger log = LoggerFactory.getLogger(SearchFactoryConverter.class);

    @Override
    public SearchService convert(String mode) {
        log.info("Mode:" + mode);
        if (mode.equalsIgnoreCase(LaunchMode.TEST.name())) {
            return new FixturedSearchService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
//            return new AwsS3StorageService();
        }
        return new FixturedSearchService();
    }
}
