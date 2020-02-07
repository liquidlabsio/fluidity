package io.precognito.services.fixture;

import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.StorageIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixturedIndexerService implements StorageIndexer {

    private final Logger log = LoggerFactory.getLogger(FixturedIndexerService.class);

    public FixturedIndexerService(){
        log.info("Created");
    }
    @Override
    public FileMeta enrichMeta(FileMeta fileMeta) {
        return fileMeta;
    }

    @Override
    public FileMeta index(FileMeta fileMeta, String cloudRegion) {
        if (fileMeta.getToTime() == 0) {
            fileMeta.setFromTime(System.currentTimeMillis() - 10000);
            fileMeta.setToTime(System.currentTimeMillis());
        }

        if (fileMeta.getFromTime() == 0) {
            fileMeta.setFromTime(fileMeta.getToTime() - (15 * 60 * 1000));
        }
        return fileMeta;
    }
}
