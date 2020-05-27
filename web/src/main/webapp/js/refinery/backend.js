$(document).ready(function () {
    refineryBackendBinding()

});

class RefineryInterface {
    submit(search, serviceAddress, modelName) {
        console.log(search + serviceAddress + modelName);
    }
    status(session, modelName) {
        console.log(session + modelName);
    }
    model(session, modelName) {
        console.log(session + modelName);
    }
}

class RefineryRest extends RefineryInterface {

    searchToForm(search) {
        var formData = new FormData();
        formData.append("origin", JSON.stringify(search.origin));
        formData.append("uid", search.uid);
        formData.append("expression", JSON.stringify(search.expression));
        formData.append("from", search.from);
        formData.append("to", search.to);
        return formData;
    }

    submit(search, modelName) {
        let self = this;
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

         jQuery.ajax({
                    type: 'POST',
                      url: SERVICE_URL + '/dataflow/submit/'
                                 + encodeURIComponent(DEFAULT_TENANT) + '/'
                                 + encodeURIComponent(modelName) + '/'
                                  + encodeURIComponent(SERVICE_URL),
                    data: self.searchToForm(search),
                    processData: false,
                    contentType: false,
                    cache : false,
                    success: function(response) {
                       $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                       $.Topic(Fluidity.Refinery.Topics.submitReply).publish(response);
                    },
                    fail: function (xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
                        $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                    }
                });


    }

    status(session, modelName) {
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

         $.get(SERVICE_URL + '/dataflow/status',
            {tenant:DEFAULT_TENANT, session: session, model: modelName},
            function(response) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                $.Topic(Fluidity.Refinery.Topics.statusReply).publish(response);
            })
            .fail(function (xhr, ajaxOptions, thrownError) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                alert("Error status: " + xhr.status + " Msg: " + thrownError);
            });
    }
    model(session, modelName) {
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();
        $.Topic(Fluidity.Explorer.Topics.startSpinner).publish();

         $.get(SERVICE_URL + '/dataflow/model',
            {tenant:DEFAULT_TENANT, session: session, model: modelName},
            function(response) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                $.Topic(Fluidity.Refinery.Topics.modelReply).publish(response);
            })
            .fail(function (xhr, ajaxOptions, thrownError) {
                $.Topic(Fluidity.Explorer.Topics.stopSpinner).publish();
                alert("Error status: " + xhr.status + " Msg: " + thrownError);
            });
    }
}

function refineryBackendBinding() {


    let backend = new RefineryRest();

    console.log("Refinery backend using:" + backend.constructor.name)

    $.Topic(Fluidity.Refinery.Topics.submit).subscribe(function(search, modelName) {
        backend.submit(search, modelName);
    })
    $.Topic(Fluidity.Refinery.Topics.status).subscribe(function(session, modelName) {
        backend.status(session, modelName);
    })
    $.Topic(Fluidity.Refinery.Topics.model).subscribe(function(session, modelName) {
        backend.model(session, modelName);
    })


}