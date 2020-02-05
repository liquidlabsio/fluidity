package io.precognito.services.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.ws.rs.*;
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

    @ConfigProperty(name = "user.hashes", defaultValue = "-906277200")
    String userHashes;

    @ConfigProperty(name = "user.domain", defaultValue = "logscape")
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
            responseBuilder.entity(System.currentTimeMillis() + ": authorised user:" + username + " time:" + new Date());
            responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, System.currentTimeMillis());
        }
        return responseBuilder.build();
    }

}
