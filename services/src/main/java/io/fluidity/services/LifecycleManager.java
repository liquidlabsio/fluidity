package io.fluidity.services;

import io.fluidity.services.aws.AwsS3StorageService;
import io.fluidity.services.query.FileMetaDataQueryService;
import io.fluidity.services.search.SearchService;
import io.fluidity.services.storage.Storage;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class LifecycleManager {


    @ConfigProperty(name = "fluidity.services.query")
    FileMetaDataQueryService query;

    @ConfigProperty(name = "fluidity.services.storage")
    Storage storageService;

    private final Logger log = LoggerFactory.getLogger(LifecycleManager.class);

    void onStart(@Observes StartupEvent ev) {
        log.info("The application is starting...");
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
        query.stop();
        storageService.stop();

    }
}
