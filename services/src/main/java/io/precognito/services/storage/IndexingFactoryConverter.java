package io.precognito.services.storage;

import io.precognito.services.fixture.FixturedIndexerService;
import org.eclipse.microprofile.config.spi.Converter;

public class IndexingFactoryConverter implements Converter<StorageIndexer> {

    @Override
    public StorageIndexer convert(String s) {
        return new FixturedIndexerService();
    }
}
