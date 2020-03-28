package io.precognito.services.query;

import io.precognito.services.aws.AWS;
import io.precognito.services.aws.AwsFileMetaDataQueryService;
import io.precognito.services.fixture.FixturedFileMetaDataQueryService;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryFactoryConverter implements Converter<FileMetaDataQueryService> {

    private final Logger log = LoggerFactory.getLogger(QueryFactoryConverter.class);
    @Override
    public FileMetaDataQueryService convert(String mode) {

        log.info(System.getProperty("mode", "not-set"));

        log.info("Mode:" + mode);

        if (mode.equalsIgnoreCase(LaunchMode.TEST.name()) || mode.equals("SERVER")) {
            return new FixturedFileMetaDataQueryService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
            return new AwsFileMetaDataQueryService();
        }
        return new FixturedFileMetaDataQueryService();
    }
}
