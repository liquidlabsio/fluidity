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
import org.graalvm.collections.Pair;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;

/**
 * Expression-Parts: [bucket | host | tags] | filename | lineMatcher-IncludeFilter | fieldExtractor | analytic | timeControl | groupby
 */
@RegisterForReflection
public class Search {

    public static String searchStagingName = "_STAGE_/";
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
    public Long from = 0l;

    @FormParam("to")
    @PartType(MediaType.TEXT_PLAIN)
    public Long to = 0l;

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

    public Pair<String, Long> getFieldNameAndValue(String sourceName, String nextLine) {
        if (fieldExtractor == null) {
            fieldExtractor = new FieldExtractor(expression);
        }
        return fieldExtractor.getSeriesNameAndValue(sourceName, nextLine);
    }
    private transient GroupByExtractor groupByExtractor;
    public String applyGroupBy(String tags, String sourceName) {
        if (groupByExtractor == null) {
            groupByExtractor = new GroupByExtractor(expression);
        }
        return groupByExtractor.applyGrouping(tags, sourceName);
    }


    public Series<Long> getTimeSeries(String seriesName, String groupBy, long from, long to) {
        String[] split = expression.split("\\|");
        String timeSeriesStyle = split.length > EXPRESSION_PARTS.timeseries.ordinal() ? split[EXPRESSION_PARTS.timeseries.ordinal()].trim() : "";

        if (timeSeriesStyle.equals("time.overlay()"))
            return new OverlayTimeSeries<>(seriesName, groupBy, from, to, new Series.LongOps());
        return new TimeSeries<>(seriesName, groupBy, from, to, new Series.LongOps());
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
            return "storage://" + bucketName + "/" + stagingPrefix() + "/" + hostnameAndPath[1];
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
