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
        this.search = search;
        $.Topic(Precognito.Search.Topics.submitSearch).publish(search);
    }
    setFileUrls(fileUrls) {
        self = this;
        this.searchFileUrls = fileUrls;
        this.searchedFileUrls = []
        this.searchFileUrls.forEach(function(fileUrl, index, arr){
            $.Topic(Precognito.Search.Topics.searchFile).publish(fileUrl, self.search)
        })

    }

    setSearchFileResults(histoUrl, eventsUrl) {
        self = this;
        console.log("Got Result, total: " + this.searchedFileUrls.length + " of :" + searchFileUrls )
        this.searchEditor.setValue("Got Result, total: " + this.searchedFileUrls.length + " of:" + this.searchFileUrls.length)

        this.searchedFileUrls.push( {
            filteredDataUrl: eventsUrl,
            histoUrl: histoUrl
        })
        if (this.searchedFileUrls.size == searchFileUrls.size) {
            $.Topic(Precognito.Search.Topics.getFinalResult).publish(self.search, self.searchedFileUrls);
        }
    }
    setFinalResult(results) {
        this.searchEditor.setValue(results);
        console.log("Finished:" + this.search)
    }
}