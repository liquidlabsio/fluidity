
$(document).ready(function () {

    backendBinding()
$.ajaxSetup({
    crossDomain: true
//    ,
//    xhrFields: {
//        withCredentials: true
//    }//,
//    username: 'test',
//    password: 'test'
});



});

class FilesInterface {

    upload(file) {
        throw new err ("not implemented")
    }

    listFiles() {
        throw new err ("not implemented")
    }

    fileContents(filename) {
        throw new err ("not implemented")
    }
    importFromStorage(storageId, includeFileMask, tags) {
        throw new err ("not implemented")
    }
}

var testFiles = [
    { filename:'/this/is/a/file.txt', tags:'prod, uk', resource:'someHost', sizeBytes:100, from:'10:00.21', to:'14:02.34'},
    { filename:'/this/is/a/fileB.txt', tags:'prod, uk', resource:'someHost', sizeBytes:100, from:'10:13.33', to:'15:55.34'}
]

var fixturedFiles = new Map([
        [testFiles[0].name, testFiles[0]],
        [testFiles[1].name, testFiles[1]],
    ]
);

class FilesFixture extends FilesInterface {

    listFiles() {
        let results = new Array();
        fixturedFiles.forEach((value, key, map) => {
            results.push(value)
        });
        $.Topic(Precognito.Explorer.Topics.setListFiles).publish(results);
    }

    fileContents(filename) {
        $.Topic(Precognito.Explorer.Topics.setFileContent).publish(
            fixturedFiles.get(filename).name + " made up file contents from the test fixture"
        );
    }
    importFromStorage(storageId, includeFileMask, tags) {
        throw new err ("not implemented")
    }
}


class RestVersion extends FilesInterface {

    downloadBinaryDataFromURL(url, filename){
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        let self=this;
        var oReq = new XMLHttpRequest();
                oReq.open("GET", url, true);
                oReq.responseType = "blob";
                oReq.onload = function(oEvent) {
                    $.Topic(Precognito.Explorer.Topics.setFileContent).publish("expanding gz...");
                  let blob = oReq.response;
                  var reader = new FileReader();
                  reader.readAsArrayBuffer(blob);
                  reader.onloadend = (event) => {
                      var byteArrayStuff = reader.result;
                      let textyBytes = pako.inflate(byteArrayStuff);
                      var explodedString = new TextDecoder("utf-8").decode(textyBytes);
                      $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                      $.Topic(Precognito.Explorer.Topics.setFileContent).publish(explodedString);
                    }
                  }
                oReq.onerror = function(err) {
                    let url = SERVICE_URL + '/query/get/' +  encodeURIComponent(DEFAULT_TENANT) + "/" + encodeURIComponent(filename)
                    $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                    self.downloadBinaryDataFromURL(url, filename);
                }
                oReq.send();
    }

    listFiles() {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/query/list', {},
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Explorer.Topics.setListFiles).publish(response);
            }
        ).fail(function(){
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
        })
    }
    importFromStorage(storageId, tags, includeFileMask, prefix, ageDays) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/storage/import', {tenant:DEFAULT_TENANT, storageId: storageId, includeFileMask: includeFileMask, tags: tags, prefix: prefix, ageDays: ageDays},
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Explorer.Topics.importedFromStorage).publish(response);
            })
        .fail(function (xhr, ajaxOptions, thrownError) {
            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
            alert("Error status: " + xhr.status + " Msg: " + thrownError);
        })
    }
    removeImportFromStorage(storageId, tags, includeFileMask, prefix, ageDays) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        $.get(SERVICE_URL + '/storage/removeImported', {tenant:DEFAULT_TENANT, storageId: storageId, includeFileMask: includeFileMask, tags: tags, prefix: prefix, ageDays: ageDays},
            function(response) {
                $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                $.Topic(Precognito.Explorer.Topics.removedImportFromStorage).publish(response);
            })
        .fail(function (xhr, ajaxOptions, thrownError) {
            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
            alert("Error status: " + xhr.status + " Msg: " + thrownError);
        })
    }


    /**
    * The S3 bucket needs CORS enabled for direct downloads to work. If it fails it retried by using a faas request
    **/
    fileContentsByURL(filename) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        let self=this;
        $.Topic(Precognito.Explorer.Topics.setFileContent).publish("loading...");
        $.get(SERVICE_URL + '/query/getDownloadUrl/' +  encodeURIComponent(DEFAULT_TENANT) + "/" + encodeURIComponent(filename),{},
            function(urlLocation) {
                try {
                    if (filename.endsWith(".gz")) {
                        self.downloadBinaryDataFromURL(urlLocation, filename);
                        $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                    } else {
                     $.get(urlLocation,{},
                        function(responseContent) {
                            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                            $.Topic(Precognito.Explorer.Topics.setFileContent).publish(responseContent);
                        })
                        .fail(function(xhr, ajaxOptions, thrownError) {
                            $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                            $.Topic(Precognito.Explorer.Topics.setFileContent).publish("load by URL failed (CORS disabled) - falling back ... still loading...");
                            self.fileContents(filename)
                        })
                    }
                } catch (err) {
                    console.log("Failed to load by signed URL - reverting to Lambda")
                    $.Topic(Precognito.Explorer.Topics.setFileContent).publish("load by URL failed - falling back ... still loading...");
                    fileContents(filename)
                }
            })
    }
    fileContents(filename) {
        $.Topic(Precognito.Explorer.Topics.startSpinner).publish();
        let self=this;
        $.Topic(Precognito.Explorer.Topics.setFileContent).publish("loading...");
        if (filename.endsWith(".gz")) {
            let url = SERVICE_URL + '/query/get/' +  encodeURIComponent(DEFAULT_TENANT) + "/" + encodeURIComponent(filename)
            self.downloadBinaryDataFromURL(url, filename);
        } else {
            $.get(SERVICE_URL + '/query/get/' +  encodeURIComponent(DEFAULT_TENANT) + "/" + encodeURIComponent(filename),{},
                function(response) {
                    $.Topic(Precognito.Explorer.Topics.stopSpinner).publish();
                    $.Topic(Precognito.Explorer.Topics.setFileContent).publish(response);
                })
            .fail(function (xhr, ajaxOptions, thrownError) {
                        alert("Error status:" + xhr.status + " Msg:" + thrownError);
                        $.Topic(Precognito.Explorer.Topics.setFileContent).publish("Failed to load file contents:" + thrownError + " status:" + xhr.status);

              })
        }

    }

    downloadFileContent(filename) {
        console.log("Downloading:" + SERVICE_URL + '/query/download/' + DEFAULT_TENANT + '/'  + filename)
        $.get(SERVICE_URL + '/query/getDownloadUrl/' +  encodeURIComponent(DEFAULT_TENANT) + "/" + encodeURIComponent(filename),{},
            function(urlLocation) {
                console.log("Signed:" + urlLocation)
                window.open(urlLocation);
            })
    }
}

function backendBinding () {
    // let backend = new FilesFixture();
    let backend = new RestVersion();

    console.log("Backend is using:" + backend.constructor.name)

    $.Topic(Precognito.Explorer.Topics.getListFiles).subscribe(function(event) {
        backend.listFiles();
    })
    $.Topic(Precognito.Explorer.Topics.getFileContent).subscribe(function(event) {
//        backend.fileContents(event);
        backend.fileContentsByURL(event);
    })

    $.Topic(Precognito.Explorer.Topics.downloadFileContent).subscribe(function(event) {
        backend.downloadFileContent(event);
    })

    $.Topic(Precognito.Explorer.Topics.importFromStorage).subscribe(function(storageId, includeFileMask, tags, prefix, ageDays) {
        backend.importFromStorage(storageId, includeFileMask, tags, prefix, ageDays);
    })
    $.Topic(Precognito.Explorer.Topics.removeImportFromStorage).subscribe(function(storageId, includeFileMask, tags, prefix, ageDays) {
        backend.removeImportFromStorage(storageId, includeFileMask, tags, prefix, ageDays);
    })
    $.Topic(Precognito.Explorer.Topics.startSpinner).subscribe(function() {
        $(".spinner").show()
    })
    $.Topic(Precognito.Explorer.Topics.stopSpinner).subscribe(function() {
        $(".spinner").hide()
    })

}

