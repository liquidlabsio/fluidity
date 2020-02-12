// define pub-sub topics
Precognito.Search.Topics = {
    submitSearch: 'submitSearch',
    setSearchFiles: 'setSearchFiles',
    searchFile: 'searchBucket',
    setSearchFileResults: 'setSearchFileResults',
    getFinalResult: 'getFinalResult',
    setFinalResult: 'setFinalResult'
}

$(document).ready(function () {

    let searcher = new Search();

    searcher.bind();

});

class Search {

    constructor() {
        this.searchId = 0;
        this.searchFileUrls = [];
        this.searchedFileUrls = [];
        this.searchEditor = ace.edit("searchResultsEditor");
        this.searchEditor.setTheme("ace/theme/monokai");
        this.searchEditor.session.setMode("ace/mode/javascript");
        this.searchEditor.session.setUseWrapMode(true);
    }
    bind() {
        let self = this;
        $('#submitSearch').click(function(){
                console.log("Submit Search")
                self.submitSearch({
                    origin: 'username',
                    uid: new Date().getTime(),
                    expression: $('#searchInput').val(),
                    from: new Date().getTime() - 1000,
                    to: new Date().getTime()
                })
            });

        $.Topic(Precognito.Search.Topics.setSearchFiles).subscribe(function(returnedFileUrls) {
            console.log("Got SearchFile URLS:" + returnedFileUrls)
            self.setFileUrls(returnedFileUrls);
        })
        $.Topic(Precognito.Search.Topics.setSearchFileResults).subscribe(function(fileResults) {
            console.log("Set Bucket Results histo & raw:" + fileResults)
            self.setSearchFileResults(fileResults[0], fileResults[1])
        })
        $.Topic(Precognito.Search.Topics.setFinalResult).subscribe(function(finalResult) {
//                console.log("Set final results:" + finalResult)
                self.setFinalResult(finalResult)
        })
        $('.searchZoom').click(function(event){
                let zoomDirection = $(event.currentTarget).data().zoom;
                let normalClass = "normalSizeEditor";
                let mediumClass = "mediumSizeEditor";
                let largeClass = "largeSizeEditor";
                let editor = $('#searchResultsEditor')
                if (editor.hasClass(normalClass)) {
                    if (zoomDirection == "in") {
                        editor.removeClass(normalClass)
                        editor.addClass(mediumClass)
                    } else {
                    // already at normal size
                    }
                } else if (editor.hasClass(mediumClass)) {
                    editor.removeClass(mediumClass)
                    if (zoomDirection == "in") {
                        editor.addClass(largeClass)
                    } else {
                        editor.addClass(normalClass)
                    }
                } else if (editor.hasClass(largeClass)) {
                  if (zoomDirection == "in") {
                  } else {
                     editor.removeClass(largeClass)
                      editor.addClass(mediumClass)
                  }
                }
                self.searchEditor.resize()
                $("#searchResultsEditor").get(0).scrollIntoView();
                return false;
        })

    }


    submitSearch(search) {
        console.log("Setting Search:"+ search)
        this.searchRequest = search;
        this.startTime =  new Date();
        $.Topic(Precognito.Search.Topics.submitSearch).publish(this.searchRequest);
    }
    setFileUrls(fileMetas) {
        console.log("Got Files:" + fileMetas)
        let self = this;
        this.searchFileMetas = fileMetas;
        this.searchedEvents = []
        this.searchedHistos = []
        this.searchFileMetas.forEach(function(fileMeta, index, arr){
            console.log("fileRequest:" + fileMeta + " index:" + index + " self:" + self)
            console.log(self)
            // TODO: look at chunking them together
            $.Topic(Precognito.Search.Topics.searchFile).publish(self.searchRequest, [fileMeta])
        })
    }

    setSearchFileResults(histoUrl, eventsUrl) {
        let self = this;
        console.log("Got Result, total: " + this.searchedHistos.length + " of :" + this.searchFileMetas.length )
        this.searchEditor.setValue("Got Result, total: " + this.searchedHistos.length + " of:" + this.searchFileMetas.length)

        this.searchedEvents.push(eventsUrl)
        this.searchedHistos.push(histoUrl)

        if (this.searchedEvents.length == this.searchFileMetas.length) {
            this.searchEditor.setValue("Got all results! Requesting aggregation:" + this.searchFileMetas.length)
            $.Topic(Precognito.Search.Topics.getFinalResult).publish(self.searchRequest, self.searchedHistos, self.searchedEvents);
        }
    }

    setFinalResult(results) {
//        console.log("Result")
//        console.log(results)
        let elapsed = new Date().getTime() - this.startTime.getTime();
        $("#searchStats").text("Events: " + Precognito.formatNumber(results[0]) + " Elapsed: " + Precognito.formatNumber(elapsed))
        this.searchEditor.setValue(results[2]);
        console.log("Finished:" + this.searchRequest)
    }
}