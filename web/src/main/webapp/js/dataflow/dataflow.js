/**
* Implements: https://github.com/liquidlabsio/fluidity/issues/62
**/
$(document).ready(function () {
    Fluidity.Dataflow.dataflow = new Dataflow();
    Fluidity.Dataflow.dataflow.bind();
});

class Dataflow {

    constructor() {

    try {
        this.uuid = new Fluidity.Util.UUID();
        this.searchId = 0;
        this.duration = 1440;

        } catch (error) {
            console.log(error)
        }
    }
    bind() {
    }
    /**
    * Load timeseries latency and volumes
    **/
    loadHistogram() {
    }
}