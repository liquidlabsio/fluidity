
// define pub-sub topics
Fluidity.Refinery.Topics = {
    submit: 'submit',
    submitReply: 'submitReply'
}

$(document).ready(function () {

    Fluidity.Refinery.refinery = new Refinery();

    Fluidity.Refinery.refinery.bind();

});

class Refinery {

    constructor() {

    try {
        this.searchId = 0;
        this.searchFileUrls = [];
        this.searchedFileUrls = [];
        this.duration = 60;

        } catch (error) {
            console.log(error)
        }
    }
    bind() {

    }
    submit() {
        console.log("Building search and submitting")
         let search = {
                    origin: 'username',
                    uid: new Date().getTime(),
                    expression: Fluidity.Search.searchInputBucket.query + '|' + Fluidity.Search.searchInputFilename.query + '|' + Fluidity.Search.searchInputRecord.query
                                + '|' +  Fluidity.Search.searchInputField.query + '|' + Fluidity.Search.searchInputAnalytic.query
                                + '|' + Fluidity.Search.searchTimeSeriesToggle.time + '|' + Fluidity.Search.searchInputGroupBy.query ,
                    from: new Date().getTime() - searcher.duration * 60 * 1000,
                    to: new Date().getTime()
                }
        search.expression =
    }
}