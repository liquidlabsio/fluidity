/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.services;

import io.fluidity.services.query.QueryService;
import io.fluidity.services.storage.Storage;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class LifecycleManager {

    /**
     * Injected here to allow it to be configured correctly in other beans that dont use CDI injections
     */
    @Inject
    DynamoDbClient dynamoDB;


    @ConfigProperty(name = "fluidity.services.query")
    QueryService query;

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
