package io.precognito.services.search;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *   uid: new Date().getTime(),
 *                     search: $('#searchInput').val(),
 *                     from: new Date().getTime() - 1000,
 *                     to: new Date().getTime()
 */
@RegisterForReflection
public class Search {

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public String origin;

    @FormParam("uid")
    @PartType(MediaType.TEXT_PLAIN)
    public String uid;

    @FormParam("expression")
    @PartType(MediaType.TEXT_PLAIN)
    public String expression;

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public long from;

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public long to;

    public String getSearchDestination(String bucketName, String searchUrl) {
        try {
            URI uri = new URI(searchUrl);
            String bucket = uri.getHost();
            String filename = uri.getPath().substring(1);
            return new URI("s3://" + bucketName + "/" + "search-staging-/" + this.uid + "/" + filename).toString();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
