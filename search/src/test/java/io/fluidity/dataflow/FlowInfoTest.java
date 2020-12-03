package io.fluidity.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class FlowInfoTest {

    @Test
    void datsToJson() throws JsonProcessingException {

        List<Map<String, Object>> dats = List.of(Map.of("service.operation", "dataflow.submit:1606387723226, builder.workflow:1606387723228, builder.workflow:1606387723229, workflow.extractFlows:1606387723230, workflow.rewriteCorrelationData:1606387723460, builder.extractFlow:1606387723462"));
        String jsonResults = FlowInfo.datsToJson(new ObjectMapper(), dats);
        assertThat(jsonResults.contains("dataflow.submit"), is(true));
    }
}