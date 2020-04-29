package io.fluidity.search;

import io.fluidity.search.agg.histo.OverlayTimeSeries;
import io.fluidity.search.agg.histo.Series;
import io.fluidity.search.agg.histo.TimeSeries;
import io.fluidity.search.field.FilenameMatcher;
import io.fluidity.search.field.TagMatcher;
import io.fluidity.search.field.extractor.FieldExtractor;
import io.fluidity.search.field.matchers.PMatcher;
import io.fluidity.search.field.matchers.RecordMatcherFactory;
import io.fluidity.util.UriUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;
import java.util.AbstractMap;

/**
 * Expression-Parts: [bucket | host | tags] | filename | lineMatcher-IncludeFilter | fieldExtractor | analytic | timeControl | groupby
 */
@RegisterForReflection
public class Search {

    public static String searchStagingName = "_STAGE_";
    public static String histoSuffix = ".histo";
    public static String eventsSuffix = ".events";

    public enum EXPRESSION_PARTS {bucket, filename, record, field, analytic, timeseries, groupby}

    @FormParam("origin")
    @PartType(MediaType.TEXT_PLAIN)
    public String origin;

    @FormParam("uid")
    @PartType(MediaType.TEXT_PLAIN)
    public String uid;

    @FormParam("expression")
    @PartType(MediaType.TEXT_PLAIN)
    public String expression = "*|*|*|*|*|*";

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

    transient TagMatcher tagMatcher;

    public boolean tagMatches(String tags) {
        if (tagMatcher == null) {
            tagMatcher = new TagMatcher(this.expression);
        }
        return tagMatcher.matches(tags);

    }


    private transient FilenameMatcher filenameMatcher;

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

    private transient FieldExtractor fieldExtractor;

    public AbstractMap.SimpleEntry<String, Object> getSeriesNameAndValue(String sourceName, String nextLine) {
        if (fieldExtractor == null) {
            fieldExtractor = new FieldExtractor(expression);
        }
        return fieldExtractor.getSeriesNameAndValue(sourceName, nextLine);
    }
    private transient GroupByExtractor groupByExtractor;
    public AbstractMap.SimpleEntry<String, Object> applyGroupBy(AbstractMap.SimpleEntry<String, Object> seriesNameAndValue, String tags, String sourceName) {
        if (groupByExtractor == null) {
            groupByExtractor = new GroupByExtractor(expression);
        }
        return groupByExtractor.applyGrouping(seriesNameAndValue, tags, sourceName);
    }


    public Series getTimeSeries(String seriesName, long from, long to) {
        String[] split = expression.split("\\|");
        String timeSeriesStyle = split.length > EXPRESSION_PARTS.timeseries.ordinal() ? split[EXPRESSION_PARTS.timeseries.ordinal()].trim() : "";

        if (timeSeriesStyle.equals("time.overlay()")) return new OverlayTimeSeries(seriesName, from, to);
        return new TimeSeries(seriesName, from, to);
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
            return "s3://" + bucketName + "/" + stagingPrefix() + "/" + hostnameAndPath[1];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String histoDestinationURI(String bucketName, String searchUrl) {
        return stagingLocation(bucketName, searchUrl) + histoSuffix;
    }

    public String stagingPrefix() {
        return searchStagingName + this.uid;
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
