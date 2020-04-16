package io.fluidity.services.storage;

import io.fluidity.services.fixture.FixturedIndexerService;
import org.eclipse.microprofile.config.spi.Converter;

public class IndexingFactoryConverter implements Converter<StorageIndexer> {

    @Override
    public StorageIndexer convert(String s) {
        return new FixturedIndexerService();
    }
}
