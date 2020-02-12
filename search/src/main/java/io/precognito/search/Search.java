package io.precognito.search;

import io.precognito.search.matchers.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 *   Expression-Parts: bucket | host | tags | filename | lineMatcher-IncludeFilter | fieldExtractor | analytic
 */
@RegisterForReflection
public class Search {

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public String origin;

    @FormParam("uid")
    @PartType(MediaType.TEXT_PLAIN)
    public String uid;

    // Parts: bucket | host | tags | filename | lineMatcher | fieldExtractor
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
            URI uri = new URI(searchUrl.replace(" ", "%20"));
            String bucket = uri.getHost();
            String filename = uri.getPath().substring(1);
            return new URI(bucketName + "/" + "search-staging/" + this.uid + "/" + filename).toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    transient PMatcher matcher;
    public boolean matches(String nextLine) {
        if (matcher == null){
            String[] split = expression.split("\\|");
            final String lineMatcherExpression = split.length > 4 ? split[4].trim() : "";
            matcher = MatcherFactory.getMatcher(lineMatcherExpression);
        }
        return matcher.matches(nextLine);
    }

    @Override
    public String toString() {
        return "Search{" +
                "origin='" + origin + '\'' +
                ", uid='" + uid + '\'' +
                ", expression='" + expression + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", matcher=" + matcher +
                '}';
    }
}
