package io.precognito.search.agg;

import io.precognito.search.Search;
import io.precognito.search.processor.Series;
import io.precognito.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleHistoCollectorTest {

    @Test
    ByteArrayOutputStream add() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long to = System.currentTimeMillis();
        long from = to - DateUtil.HOUR;
        SimpleHistoCollector collector = new SimpleHistoCollector(baos, "someFile", "tags", "s3://stuff", new Search(), from, to);
        collector.add(from, 100, "Line");
        collector.add(from, 100, "Line");
        collector.add(from, 100, "Line");
        collector.close();
        Series series = collector.series;
        long lineCountWithDefaultFunction = series.get(from);
        assertEquals(3, lineCountWithDefaultFunction);

        return baos;
    }

    @Test
    void close() throws Exception {
        ByteArrayOutputStream baos = add();
        String series = new String(baos.toByteArray());
        System.out.println(series);
        assertTrue(series.contains("someFile"),"Json message didnt contain seriesName - 'someFile");

    }
}