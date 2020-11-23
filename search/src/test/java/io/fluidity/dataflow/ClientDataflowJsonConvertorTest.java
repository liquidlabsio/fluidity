package io.fluidity.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDataflowJsonConvertorTest {

    @Test
    void flowInfosToDurations() throws IOException {
        ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1l, System.currentTimeMillis(), 100l, 100l);
        byte[] json = convertor.toClientFlowsList(List.of(new FlowInfo("flowId", List.of("file1"),
                Collections.singletonList(new Long[]{1l, 1000l}))));
        System.out.println("JSON:" + json);
        System.out.println(new String(json));
        ObjectMapper objectMapper = new ObjectMapper();
        List list = objectMapper.readValue(json, List.class);

        System.out.println("GOT:" + list);
        // 2 rows, 1 columns list, 1 duration list
        assertThat(list.size(), equalTo(2));
    }

    @Test
    void shouldMatchTimesStart() {
        final ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1000L, 2000L,
                10L, -1L);
        assertThat(convertor.isMatch("someFlow/flow_999_1000_e7d6fd9b-50ea-41c2-a9f1-c8af5a38b980_.flow"), is(true));
    }
    @Test
    void shouldMatchTimesOutside() {
        final ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1000L, 2000L,
                10L, -1L);
        assertThat(convertor.isMatch("someFlow/flow_999_2100_e7d6fd9b-50ea-41c2-a9f1-c8af5a38b980_.flow"), is(true));
    }
    @Test
    void shouldMatchTimesInside() {
        final ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1000L, 2000L,
                10L, -1L);
        assertThat(convertor.isMatch("someFlow/flow_1100_1900_e7d6fd9b-50ea-41c2-a9f1-c8af5a38b980_.flow"),
                is(true));
    }
}