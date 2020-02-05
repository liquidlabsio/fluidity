package io.precognito.services;

import io.precognito.services.fixture.FixturedIndexerService;
import io.precognito.services.storage.StorageIndexer;
import org.eclipse.microprofile.config.spi.Converter;

public class IndexingFactoryConverter implements Converter<StorageIndexer> {

    @Override
    public StorageIndexer convert(String s) {
        return new FixturedIndexerService();
    }
}
