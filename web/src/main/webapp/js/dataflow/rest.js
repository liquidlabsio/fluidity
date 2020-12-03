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
    dataflowsForTime(self, session, modelName, timeX1, timeX2, valueY, responseCallback){
            $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

             $.get(SERVICE_URL + '/dataflow/client/dataflows',
                {tenant:DEFAULT_TENANT, session: session, model: modelName, timeX1:timeX1, timeX2:timeX2, valueY:valueY},
                function(response) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    responseCallback(self, response)
                })
                .fail(function (xhr, ajaxOptions, thrownError) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    alert("Error status: " + xhr.status + " Msg: " + thrownError);
                });

    }
    dataflowsForCorrelation(self, session, modelName, correlation, responseCallback){
            $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

             $.get(SERVICE_URL + '/dataflow/client/dataflow',
                {tenant:DEFAULT_TENANT, session: session, model: modelName, correlation:correlation},
                function(response) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    responseCallback(self, response)
                })
                .fail(function (xhr, ajaxOptions, thrownError) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    alert("Error status: " + xhr.status + " Msg: " + thrownError);
                });

    }
    loadCorrelation(self, session, modelName, correlation, responseCallback){
            $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

             $.get(SERVICE_URL + '/dataflow/client/correlation',
                {tenant:DEFAULT_TENANT, session: session, model: modelName, correlation:correlation},
                function(response) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    responseCallback(self, response.value)
                })
                .fail(function (xhr, ajaxOptions, thrownError) {
                    $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    alert("Error status: " + xhr.status + " Msg: " + thrownError);
                });

    }
}
