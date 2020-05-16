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

@Path("/dataflow")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface DataflowService {
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
    @Path("/model")
    List<String> model(@QueryParam("tenant") String tenant, @QueryParam("session") String session,
                       @QueryParam("model") String modelName);

    @POST
    @Path("/rewrite/{tenant}/{session}/{files}/{modelPath}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String rewriteCorrelationData(@PathParam("tenant") String tenant, @PathParam("session") String session,
                                  @PathParam("files") String fileMetas, @PathParam("modelPath") String modelPath,
                                  @MultipartForm Search search);
}
