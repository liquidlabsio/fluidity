
$(document).ready(function () {
    searchBackendBinding()

});

var searchFileUrls = [
    "s3://bucket/path.txt", "s3://bucket/path2.txt"
]
var testFileUrls = [
    "s3://bucket/path.txt", "s3://bucket/path2.txt"
]
var searchFileResults = [ "s3://file1.raw", "s3file1.histo_10m"]
var fixturedFiles = new Map([
        [testFiles[0].name, testFiles[0]],
        [testFiles[1].name, testFiles[1]],
    ]
);

class SearchInterface {
    submitSearch(search) {
    }
    searchFile(fileUrl, search) {
    }
    getFinalResult(searchId, searchedFiles) {
    }
}


class SearchFixture extends SearchInterface {
    submitSearch(search) {
        $.Topic(Precognito.Search.Topics.setSearchFiles).publish(searchFileUrls);
    }
    searchFile(search, fileUrl) {
        $.Topic(Precognito.Search.Topics.setSearchFileResults).publish(searchFileResults);
    }
    getFinalResult(search, searchedFiles) {
        $.Topic(Precognito.Search.Topics.setFinalResult).publish("We got results");
    }
}


class SearchRest extends SearchInterface {

    searchToForm(search) {
        var formData = new FormData();
//        formData.append("origin", JSON.stringify(search.origin));
        formData.append("uid", search.uid);
        formData.append("expression", JSON.stringify(search.expression));
        formData.append("from", search.from);
        formData.append("to", search.to);
        return formData;
    }

    submitSearch(search) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        jQuery.ajax({
            type: 'POST',
            url: SERVICE_URL + '/search/submit',
            contentType: 'application/json',
            data: JSON.stringify(search),
            dataType: 'json',
            success: function(response) {
                                       $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                                       $.Topic(Precognito.Search.Topics.setSearchFiles).publish(response);
                                   }
            ,
            fail: function (xhr, ajaxOptions, thrownError) {
                                   alert(xhr.status);
                                   alert(thrownError);
                                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();

            }
        });
    }
    searchFile(search, fileMetasArray) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        let self = this;
        let formData = this.searchToForm(search);

        jQuery.ajax({
            type: 'POST',
            url: SERVICE_URL + '/search/files/'
                + encodeURIComponent(DEFAULT_TENANT)
                + "/"+ encodeURIComponent( JSON.stringify(fileMetasArray)),
            contentType: 'multipart/form-data',
            data: self.searchToForm(search),
            processData: false,
            contentType: false,
            cache : false,
            success: function(response) {
               $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
               $.Topic(Precognito.Search.Topics.setSearchFileResults).publish(response);
            },
            fail: function (xhr, ajaxOptions, thrownError) {
                console.log(xhr.status);
                console.log(thrownError);
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
            }
        });
    }
    getFinalResult(search, histoFiles, eventFiles) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        let self = this;
        let formData = this.searchToForm(search);

        jQuery.ajax({
            type: 'POST',
            url: SERVICE_URL + '/search/finalize/'
                    + encodeURIComponent(DEFAULT_TENANT)
                    + "/" + encodeURIComponent(histoFiles)
                    + "/" + encodeURIComponent(eventFiles),
            contentType: 'multipart/form-data',
            data: self.searchToForm(search),
            processData: false,
            contentType: false,
            cache : false,
            success: function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Search.Topics.setFinalResult).publish(response);
            },
            fail: function (xhr, ajaxOptions, thrownError) {
                console.log(xhr.status);
                console.log(thrownError);
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
            }
        });
    }

}

function searchBackendBinding() {
//     let backend = new SearchFixture();
    let backend = new SearchRest();

    console.log("Backend is using:" + backend.constructor.name)

    $.Topic(Precognito.Search.Topics.submitSearch).subscribe(function(search) {
        backend.submitSearch(search);
    })
    $.Topic(Precognito.Search.Topics.searchFile).subscribe(function(search, files) {
        backend.searchFile(search, files);
    })
    $.Topic(Precognito.Search.Topics.getFinalResult).subscribe(function(search, eventFiles, histoFiles) {
        backend.getFinalResult(search, eventFiles, histoFiles);
    })

}

