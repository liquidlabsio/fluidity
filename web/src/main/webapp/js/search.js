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
                console.log("Set final results:" + finalResult)
                self.setFinalResult(finalResult)
        })
    }


    submitSearch(search) {
        console.log("Setting Search:"+ search)
        this.searchRequest = search;
        $.Topic(Precognito.Search.Topics.submitSearch).publish(this.searchRequest);
    }
    setFileUrls(fileUrls) {
        console.log("Got Files:" + fileUrls)
        let self = this;
        this.searchFileUrls = fileUrls;
        this.searchedFileUrls = []
        this.searchFileUrls.forEach(function(fileUrl, index, arr){
            console.log("fileRequest:" + fileUrl + " index:" + index + " self:" + self)
            console.log(self)
            // TODO: look at chunking them together
            $.Topic(Precognito.Search.Topics.searchFile).publish(self.searchRequest, [fileUrl])
        })
    }

    setSearchFileResults(histoUrl, eventsUrl) {
        let self = this;
        console.log("Got Result, total: " + this.searchedFileUrls.length + " of :" + searchFileUrls )
        this.searchEditor.setValue("Got Result, total: " + this.searchedFileUrls.length + " of:" + this.searchFileUrls.length)

        this.searchedFileUrls.push( {
            filteredDataUrl: eventsUrl,
            histoUrl: histoUrl
        })
        if (this.searchedFileUrls.size == searchFileUrls.size) {
            $.Topic(Precognito.Search.Topics.getFinalResult).publish(self.searchRequest, self.searchedFileUrls);
        }
    }
    setFinalResult(results) {
        this.searchEditor.setValue(JSON.stringify(results));
        console.log("Finished:" + this.searchRequest)
    }
}