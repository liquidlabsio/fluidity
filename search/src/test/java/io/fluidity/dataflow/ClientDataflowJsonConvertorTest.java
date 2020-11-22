package io.fluidity.dataflow;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDataflowJsonConvertorTest {

    @Test
    void toFromJson() {
        ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1l, System.currentTimeMillis(), 100l, 100l);
        byte[] json = convertor.toJson(Collections.singletonList(new FlowInfo("flowId", Collections.singletonList("file1"), Collections.singletonList(new Long[]{1l, 1000l}))));
        System.out.println("JSON:" + json);
        FlowInfo[] flowInfo = convertor.fromJson(json);

        assertEquals(flowInfo.length, 1);
        assertEquals(flowInfo[0].flowId, "flowId");
    }
    @Test
    void toClientJson() {
        final ClientDataflowJsonConvertor convertor = new ClientDataflowJsonConvertor(1l, System.currentTimeMillis(),
                100l, 100l);
        FlowInfo flowInfo = new FlowInfo("flowId", Collections.singletonList("file1"), Collections.singletonList(new Long[]{1l, 1000l}));
        String flowInfoItem = convertor.rewriteToClientJson(flowInfo);
        assertEquals(flowInfoItem, "['flowId',999]");
    }


}