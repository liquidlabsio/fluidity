// define pub-sub topics
Precognito.Search.Topics = {
    submitSearch: 'submitSearch',
    setSearchFiles: 'setSearchFiles',
    searchFile: 'searchBucket',
    setSearchFileResults: 'setSearchFileResults',
    getFinalEvents: 'getFinalEvents',
    setFinalEvents: 'setFinalEvents',
    getFinalHisto: 'getFinalHisto',
    setFinalHisto: 'setFinalHisto',
    setHoverInfo: 'setHoverInfo',
    prepareExplorerToOpen: 'prepareExplorerToOpen'
}

$(document).ready(function () {

    searcher = new Search();

    searcher.bind();

});

class Search {

    constructor() {

    try {
        this.searchId = 0;
        this.searchFileUrls = [];
        this.searchedFileUrls = [];
        this.searchEditor = ace.edit("searchResultsEditor");
        this.searchEditor.setTheme("ace/theme/monokai");
        this.searchEditor.session.setMode("ace/mode/javascript");
        this.searchEditor.session.setUseWrapMode(true);
        this.duration = 60;

        this.hoverLink = new HoverLink(this.searchEditor);
        } catch (error) {
            console.log(error)
        }
    }
    bind() {
        let self = this;

        $.Topic(Precognito.Search.Topics.setSearchFiles).subscribe(function(returnedFileUrls) {
            console.log("Got SearchFile URLS:" + returnedFileUrls)
            self.setFileUrls(returnedFileUrls);
        })
        $.Topic(Precognito.Search.Topics.setSearchFileResults).subscribe(function(fileResults) {
            console.log("Set Bucket Results histo & raw:" + fileResults)
            self.setSearchFileResults(fileResults[0], fileResults[1])
        })
        $.Topic(Precognito.Search.Topics.setFinalEvents).subscribe(function(events) {
                self.setFinalEvents(events)
        })
        $.Topic(Precognito.Search.Topics.setFinalHisto).subscribe(function(events) {
                self.setFinalHisto(events)
        })
        $.Topic(Precognito.Search.Topics.setHoverInfo).subscribe(function(event) {
            // expecting event '3:1581603492991:19216:'
           let parts = event.split(":");
           let filename = self.fileLut[parseInt(parts[0])]
           let time = parseInt(parts[1]);
           let offset = parts[2];
           searchFileToOpenInfo.searchFileInfo = filename + " @" + new Date(time).toLocaleString() + " - " + offset
        })
        $.Topic(Precognito.Search.Topics.prepareExplorerToOpen).subscribe(function(event) {
            // expecting; 3:1581603492991:19216:
           let parts = event.split(":");
           let filename = self.fileLut[parseInt(parts[0])]
           let time = parseInt(parts[1]);
           let offset = parts[2];
           $.Topic(Precognito.Explorer.Topics.setExplorerToOpen).publish([ filename, offset, time ])
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

        $(".search-duration-dropdown").click(function(event) {
            self.duration = $(event.currentTarget).data().duration;
            $("#searchDurationSelector").html(event.currentTarget.text);
        })

    }


    submitSearch(search) {
        console.log("Setting Search:"+ search)
        this.searchRequest = search;
        this.startTime =  new Date();
        searchChart.series = [];
        $.Topic(Precognito.Search.Topics.submitSearch).publish(this.searchRequest);
        searchFileToOpenInfo.searchFileInfo = ""
    }
    setFileUrls(fileMetas) {
        console.log("Got Files:" + fileMetas)
        let self = this;
        this.searchFileMetas = fileMetas;
        this.searchedEvents = []
        this.searchedHistos = []
        this.searchFileMetas.forEach(function(fileMeta, index, arr){
            console.log("fileRequest:" + fileMeta + " index:" + index + " self:" + self)
            // TODO: look at chunking them together
            $.Topic(Precognito.Search.Topics.searchFile).publish(self.searchRequest, [fileMeta])
        })
    }

    setSearchFileResults(histoUrl, eventsUrl) {
        let self = this;
         searchStats.stats = "Processed " + this.searchedHistos.length + " of " + this.searchFileMetas.length + " files"

        this.searchedEvents.push(eventsUrl)
        this.searchedHistos.push(histoUrl)

        if (this.searchedEvents.length == this.searchFileMetas.length) {
            searchStats.stats = "Got all results! Aggregating results:" + this.searchFileMetas.length
            $.Topic(Precognito.Search.Topics.getFinalEvents).publish(self.searchRequest, 0);
            $.Topic(Precognito.Search.Topics.getFinalHisto).publish(self.searchRequest);
        }
    }

    setFinalEvents(results) {
        let elapsed = new Date().getTime() - this.startTime.getTime();
        searchStats.stats = "Events: " + Precognito.formatNumber(results[0]) + " Elapsed: " + Precognito.formatNumber(elapsed)
        this.fileLut = $.parseJSON(results[2]);
        this.searchEditor.setValue(results[1]);
    }
    setFinalHisto(results) {
        searchChart.series = results;
    }

}