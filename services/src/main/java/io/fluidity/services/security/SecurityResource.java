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

package io.fluidity.services.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * Lame sec impl but provide something
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecurityResource {

    // the hash of 'secret'
    @ConfigProperty(name = "user.hashes", defaultValue = "-906277200")
    String userHashes;

    @ConfigProperty(name = "user.domain", defaultValue = "fluidity")
    String userDomain;


    @GET
    @Path("/id")
    public String id() {
        return SecurityResource.class.getCanonicalName();
    }

    @POST
    @Path("/login")
//    @Consumes("application/x-www-form-urlencoded")
//    public Response saveEmp(@FormParam("id") String id
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response authenticate(@FormParam("username") String username, @FormParam("password") String password) {
        int authCode = new UserMeta(username, password).auth(userDomain, userHashes) ? 200 : 401;
        Response.ResponseBuilder responseBuilder = Response.status(authCode);
        if (authCode == 401) {
            responseBuilder.entity("Failed to authorised user:" + username + " check your username and password");
            responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, "fail");
        } else {
            responseBuilder.entity(System.currentTimeMillis() + ":authorised-user:" + username + ":time:" + new Date());
            responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, System.currentTimeMillis());
        }
        return responseBuilder.build();
    }

}
