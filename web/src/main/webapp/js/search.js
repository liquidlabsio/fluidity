// define pub-sub topics
Fluidity.Search.Topics = {
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
        this.searchEditor.session.setMode("ace/mode/json");
        this.searchEditor.session.setUseWrapMode(true);
        this.duration = 60;

        this.hoverLink = new HoverLink(this.searchEditor);
        } catch (error) {
            console.log(error)
        }
    }
    bind() {
        let self = this;

        $.Topic(Fluidity.Search.Topics.setSearchFiles).subscribe(function(returnedFileUrls) {
            console.log("Got SearchFile URLS:" + returnedFileUrls)
            self.setFileUrls(returnedFileUrls);
        })
        $.Topic(Fluidity.Search.Topics.setSearchFileResults).subscribe(function(fileResults) {
//            console.log("Set Bucket Results histo & raw:" + fileResults)
            self.setSearchFileResults(fileResults[0], fileResults[1], fileResults[2])
        })
        $.Topic(Fluidity.Search.Topics.setFinalEvents).subscribe(function(events) {
                self.setFinalEvents(events)
        })
        $.Topic(Fluidity.Search.Topics.setFinalHisto).subscribe(function(events) {
                self.setFinalHisto(events)
        })
        $.Topic(Fluidity.Search.Topics.setHoverInfo).subscribe(function(event) {
            // expecting event '3:1581603492991:19216:'
           let parts = event.split(":");
           let filename = self.fileLut[parseInt(parts[0])]
           let time = parseInt(parts[1]);
           let offset = parts[2];
           searchFileToOpenInfo.searchFileInfo = filename + " @" + new Date(time).toLocaleString() + " - " + offset
        })
        $.Topic(Fluidity.Search.Topics.prepareExplorerToOpen).subscribe(function(event) {
            // expecting; 3:1581603492991:19216:
           let parts = event.split(":");
           let filename = self.fileLut[parseInt(parts[0])]
           let time = parseInt(parts[1]);
           let offset = parts[2];
           $.Topic(Fluidity.Explorer.Topics.setExplorerToOpen).publish([ filename, offset, time ])
        })


        /**
        * Expand/Collapse inputs on focus
        **/
        $('#searchInputForm input').on('focusin', function(arg){
            $(arg.target.parentElement).addClass("big-input");
        });
        $('#searchInputForm input').on('focusout', function(arg){
            $(arg.target.parentElement).removeClass("big-input");
        });

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
        $.Topic(Fluidity.Search.Topics.submitSearch).publish(this.searchRequest);
        searchFileToOpenInfo.searchFileInfo = ""
    }
    setFileUrls(fileMetas) {
        console.log("Got Files:" + fileMetas)
        let self = this;
        this.searchFileMetas = fileMetas;
        this.totalEvents = 0;
        this.searchedEvents = []
        this.searchedHistos = []
        this.searchFileMetas.forEach(function(fileMeta, index, arr){
            //console.log("fileRequest:" + fileMeta + " index:" + index + " self:" + self)
            $.Topic(Fluidity.Search.Topics.searchFile).publish(self.searchRequest, [fileMeta])
        })
    }

    setSearchFileResults(histoUrl, processedEventCount, totalEventCount) {
        let self = this;
         searchStats.stats = "Processed " + this.searchedHistos.length + " of " + this.searchFileMetas.length + " sources"

        this.searchedEvents.push(parseInt(totalEventCount))
        this.searchedHistos.push(histoUrl)

        if (this.searchedEvents.length == this.searchFileMetas.length) {
            this.totalEvents =  this.searchedEvents.reduce(function(a, b){
                        return a + b;
            }, 0);
            searchStats.stats = "Got all results! Aggregating results:" + this.searchFileMetas.length + " Total Events:" + this.totalEvents
            $.Topic(Fluidity.Search.Topics.getFinalEvents).publish(self.searchRequest, 0);
            $.Topic(Fluidity.Search.Topics.getFinalHisto).publish(self.searchRequest);
        }
    }

    setFinalEvents(results) {
        let elapsed = new Date().getTime() - this.startTime.getTime();
        searchStats.stats = "Display " + Fluidity.formatNumber(results[0]) + " events from: " + Fluidity.formatNumber(this.totalEvents) + " Elapsed: " + Fluidity.formatNumber(elapsed)
        this.fileLut = $.parseJSON(results[2]);
        this.searchEditor.setValue(results[1]);
    }
    setFinalHisto(results) {
        searchChart.series = results;
    }

}