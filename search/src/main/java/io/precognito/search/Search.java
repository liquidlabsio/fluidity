package io.precognito.search;

import io.precognito.search.matchers.PMatcher;
import io.precognito.search.matchers.RecordMatcherFactory;
import io.precognito.util.UriUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;

/**
 * Expression-Parts: [bucket | host | tags] | filename | lineMatcher-IncludeFilter | fieldExtractor | analytic
 */
@RegisterForReflection
public class Search {

    public static String searchStagingName = "_STAGE_";
    public static String histoSuffix = ".histo";
    public static String eventsSuffix = ".events";

    enum EXPRESSION_PARTS {bucket, filename, record, field, analytic}

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public String origin;

    @FormParam("uid")
    @PartType(MediaType.TEXT_PLAIN)
    public String uid;

    @FormParam("expression")
    @PartType(MediaType.TEXT_PLAIN)
    public String expression;

    @FormParam("from")
    @PartType(MediaType.TEXT_PLAIN)
    public long from;

    @FormParam("to")
    @PartType(MediaType.TEXT_PLAIN)
    public long to;



    transient PMatcher matcher;

    public boolean matches(String nextLine) {
        if (matcher == null) {
            String[] split = expression.split("\\|");
            final String lineMatcherExpression = split.length > EXPRESSION_PARTS.record.ordinal() ? split[EXPRESSION_PARTS.record.ordinal()].trim() : "";
            matcher = RecordMatcherFactory.getMatcher(lineMatcherExpression);
        }
        return matcher.matches(nextLine);
    }

    transient FilenameMatcher filenameMatcher;

    public boolean fileMatches(String filename, long from, long to) {
        if (filenameMatcher == null) {
            filenameMatcher = new FilenameMatcher(this.expression, this.from, this.to);
        }
        return filenameMatcher.matches(filename, from, to);
    }

    public String analyticValue() {
        String[] split = expression.split("\\|");
        return split.length > EXPRESSION_PARTS.analytic.ordinal() ? split[EXPRESSION_PARTS.analytic.ordinal()].trim() : "";
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

    public String eventsDestinationURI(String bucketName, String searchUrl) {
        return stagingLocation(bucketName, searchUrl) + eventsSuffix;
    }

    private String stagingLocation(String bucketName, String searchUrl) {
        try {
            searchUrl = searchUrl.replace(" ", "%20");
            String[] hostnameAndPath = UriUtil.getHostnameAndPath(searchUrl);
            return "s3://" + bucketName + "/" + searchStagingName + "/" + this.uid + "/" + hostnameAndPath[1];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String histoDestinationURI(String bucketName, String searchUrl) {
        return stagingLocation(bucketName, searchUrl) + histoSuffix;
    }

    public String stagingPrefix() {
        return searchStagingName + "/" + this.uid;
    }

    public void decodeJsonFields() {
        if (this.origin.startsWith("\"")) {
            this.origin = this.origin.substring(1, this.origin.length() - 1);
        }
        if (this.expression.startsWith("\"")) {
            this.expression = this.expression.substring(1, this.expression.length() - 1);
        }
    }

}
