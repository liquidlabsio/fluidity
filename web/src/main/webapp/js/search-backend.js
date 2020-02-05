
$(document).ready(function () {
    binding()

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
    searchFile(fileUrl, search) {
        $.Topic(Precognito.Search.Topics.setSearchFileResults).publish(searchFileResults);
    }
    getFinalResult(search, searchedFiles) {
        $.Topic(Precognito.Search.Topics.setFinalResult).publish("We got results");
    }
}


class SearchRest extends SearchInterface {

    submitSearch(search) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/search/submit', search,
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Search.Topics.setSearchFiles).publish(response);
            }
        ).fail(
            function (xhr, ajaxOptions, thrownError) {
               alert(xhr.status);
               alert(thrownError);
            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
        })
    }
    searchFile(fileUrl, search) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/search/file/', { fileUrl: fileUrl, search: search },
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Search.Topics.setSearchFileResults).publish(response);
            }
        ).fail(
            function (xhr, ajaxOptions, thrownError) {
               alert(xhr.status);
               alert(thrownError);
            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
        })
    }
    getFinalResult(search, searchedFiles) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/search/finalize', { search: search, searchedFiles: searchedFiles},
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Logscape.Search.Topics.setFinalResult).publish(response);
            }
        ).fail(
            function (xhr, ajaxOptions, thrownError) {
               alert(xhr.status);
               alert(thrownError);
            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
        })
    }

}

function binding () {
     let backend = new SearchFixture();
//    let backend = new SearchRest();

    console.log("Backend is using:" + backend.constructor.name)

    $.Topic(Precognito.Search.Topics.submitSearch).subscribe(function(search) {
        backend.submitSearch(search);
    })
    $.Topic(Precognito.Search.Topics.searchFile).subscribe(function(fileUrl, search) {
        backend.searchFile(fileUrl, search);
    })
    $.Topic(Precognito.Search.Topics.getFinalResult).subscribe(function(search, searchedFiles) {
        backend.getFinalResult(search, searchedFiles);
    })

}

