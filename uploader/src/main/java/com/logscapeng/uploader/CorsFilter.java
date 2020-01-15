package com.logscapeng.uploader;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Using a CORSFilter because  web-pages will be statically hosted on S3 or similar and force cross-resource requests
 * ContainerResponseFilter is explicitly annotated with @Provider to be discovered by the JAX-RS runtime
 *  ‘Access-Control-Allow-*‘ header with ‘*', that means any URL endpoints to this server instance can be accessed via any domain; if we want to restrict the cross-domain access explicitly, we have to mention that domain in this header
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add(
                "Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add(
                "Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add(
                "Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        responseContext.getHeaders().add(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

}
