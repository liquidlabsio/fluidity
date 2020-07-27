class DataflowRest  {

    volume(session, modelName, time, responseCallback) {
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

         $.get(SERVICE_URL + '/dataflow/client/volume',
            {tenant:DEFAULT_TENANT, session: session, model: modelName, time:time},
            function(response) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                responseCallback(response)
            })
            .fail(function (xhr, ajaxOptions, thrownError) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                alert("Error status: " + xhr.status + " Msg: " + thrownError);
            });
    }
    heatmap(session, modelName, time, responseCallback) {
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

         $.get(SERVICE_URL + '/dataflow/client/heatmap',
            {tenant:DEFAULT_TENANT, session: session, model: modelName, time:time},
            function(response) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                responseCallback(response)
            })
            .fail(function (xhr, ajaxOptions, thrownError) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                alert("Error status: " + xhr.status + " Msg: " + thrownError);
            });
    }
    dataflowsForTime(timeX, valueY, responseCallback){
            $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

             $.get(SERVICE_URL + '/dataflow/client/dataflowsForTime',
                {tenant:DEFAULT_TENANT, session: session, model: modelName, time:timeX, valueY:valueY},
                function(response) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    responseCallback(response)
                })
                .fail(function (xhr, ajaxOptions, thrownError) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    alert("Error status: " + xhr.status + " Msg: " + thrownError);
                });

    }
}
