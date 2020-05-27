/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.storage;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * First (naive) implementation.
 */
@Path("/storage")
public class StorageResource {

    private final Logger log = LoggerFactory.getLogger(StorageResource.class);

    @ConfigProperty(name = "cloud.region", defaultValue = "eu-west-2")
    String cloudRegion;

    @ConfigProperty(name = "fluidity.services.storage")
    public
    Storage storage;

    @ConfigProperty(name = "fluidity.services.indexer")
    StorageIndexer indexer;

    @ConfigProperty(name = "fluidity.services.query")
    public QueryService query;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String id() {
        return StorageResource.class.getCanonicalName();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@MultipartForm FileMeta fileMeta) {

        try {
            log.debug("uploadFile:" + fileMeta);

            fileMeta.size = Long.valueOf(fileMeta.fileContent.length);

            // this series of actions should be put on an event queue
            FileMeta indexedFile = indexer.enrichMeta(fileMeta);
            FileMeta storedAndIndexedFile = storage.upload(cloudRegion, indexedFile);
            // ideally we would trigger an indexing function from the S3 bucket write.
            // for now Im doing it in process here.
            FileMeta stored = indexer.index(storedAndIndexedFile, cloudRegion);

            query.put(stored);
            stored.fileContent = new byte[0];

            Response.ResponseBuilder responseBuilder = Response.status(200).entity("Uploaded and Indexed:" + stored);
            return responseBuilder.build();
        } catch (Exception ex) {
            log.error("Error handling request:" + fileMeta, ex);
            Response.ResponseBuilder responseBuilder = Response.status(500).entity(ex.getMessage());
            return responseBuilder.build();
        }
    }

    @GET
    @Path("/import")
    @Produces(MediaType.APPLICATION_JSON)
    public int importFromStorage(@QueryParam("tenant") String tenant, @QueryParam("storageId") String storageId,
                                 @QueryParam("includeFileMask") String includeFileMask, @QueryParam("tags") String tags,
                                 @QueryParam("prefix") String prefix, @QueryParam("ageDays") int ageDays, @QueryParam("timeFormat") String timeFormat) {

        log.debug("importFromStorage");
        try {
            List<FileMeta> imported = storage.importFromStorage(cloudRegion, tenant, storageId, prefix, ageDays, includeFileMask, tags, timeFormat);

            // newest first
            Collections.sort(imported, (o1, o2) -> Long.compare(o2.toTime, o1.toTime));

            query.putList(imported);

            log.info("Imported from Bucket:{} Amount:{}", storageId, imported.size());
            return imported.size();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    @GET
    @Path("/removeImported")
    @Produces(MediaType.APPLICATION_JSON)
    public int removeByStorageId(@QueryParam("tenant") String tenant, @QueryParam("storageId") String storageId,
                                 @QueryParam("includeFileMask") String includeFileMask, @QueryParam("tags") String tags,
                                 @QueryParam("prefix") String prefix, @QueryParam("ageDays") String ageDays) {

        log.info("removeByStorageId");
        if (ageDays.length() == 0) ageDays = "0";

        List<FileMeta> query = this.query.query(tenant, includeFileMask, tags);
        this.query.deleteList(query);
        log.info("Removed:{}", query.size());
        return query.size();
    }

}
