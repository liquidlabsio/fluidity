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

package io.fluidity.services.dataflow;

import io.fluidity.search.Search;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/dataflow")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface DataflowService {

    @GET
    @Path("/id")
    @Produces(MediaType.TEXT_PLAIN)
    String id();

    @POST
    @Path("/submit/{tenant}/{modelName}/{serviceAddress}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String submit(@PathParam("tenant") String tenant, @MultipartForm Search search,
                  @PathParam("modelName") String modelName, @PathParam("serviceAddress") String serviceAddress);

    @GET
    @Path("/status")
    String status(@QueryParam("tenant") String tenant, @QueryParam("session") String session,
                  @QueryParam("model") String modelName);

    @GET
    @Path("/model/list")
    List<String> listModels(@QueryParam("tenant") String tenant);

    @GET
    @Path("/model/load")
    String loadModel(@QueryParam("tenant") String tenant, @QueryParam("model") String modelName);

    @GET
    @Path("/model/save")
    String saveModel(@QueryParam("tenant") String tenant, @QueryParam("model") String modelName, @QueryParam("data") String modelData);

    @GET
    @Path("/model")
    List<Map<String, String>> modelDataList(@QueryParam("tenant") String tenant, @QueryParam("session") String session,
                                    @QueryParam("model") String modelName);

    @POST
    @Path("/rewrite/{tenant}/{session}/{files}/{modelPath}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String rewriteCorrelationData(@PathParam("tenant") String tenant, @PathParam("session") String session,
                                  @PathParam("files") String fileMetas, @PathParam("modelPath") String modelPath,
                                  @MultipartForm Search search);


    @GET
    @Path("/client/volume")
    String volumeHisto(@QueryParam("tenant") String tenant, @QueryParam("model") String modelName, @QueryParam("time") Long time);

    @GET
    @Path("/client/heatmap")
    String heatmapHisto(@QueryParam("tenant") String tenant, @QueryParam("model") String modelName, @QueryParam("time") Long time);

}
