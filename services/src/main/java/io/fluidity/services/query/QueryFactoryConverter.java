package io.fluidity.services.query;

import io.fluidity.services.aws.AWS;
import io.fluidity.services.aws.AwsFileMetaDataQueryService;
import io.fluidity.services.fixture.FixturedFileMetaDataQueryService;
import io.fluidity.services.server.RocksDBFileMetaDataQueryService;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryFactoryConverter implements Converter<FileMetaDataQueryService> {

    private final Logger log = LoggerFactory.getLogger(QueryFactoryConverter.class);
    private static FileMetaDataQueryService queryService = null;
    @Override
    public FileMetaDataQueryService convert(String mode) {

        if (queryService != null) return queryService;

        log.info(System.getProperty("mode", "not-set"));

        log.info("Mode:" + mode);

        if (mode.equals("SERVER")) {
            queryService = new RocksDBFileMetaDataQueryService();
        } else if (mode.equalsIgnoreCase(AWS.CONFIG)) {
            queryService = new AwsFileMetaDataQueryService();
        } else {
            queryService = new FixturedFileMetaDataQueryService();
        }
        return queryService;
    }
}
