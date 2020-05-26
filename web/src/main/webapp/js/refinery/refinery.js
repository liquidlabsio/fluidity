
// pub-sub topics
Fluidity.Refinery.Topics = {
    submit: 'submit',
    submitReply: 'submitReply',
    status: 'status',
    statusReply: 'statusReply',
    model: 'model',
    modelReply: 'modelReply'
}

$(document).ready(function () {

    Fluidity.Refinery.refinery = new Refinery();

    Fluidity.Refinery.refinery.bind();

});

class Refinery {

    constructor() {

    try {
        this.uuid = new Fluidity.Util.UUID();
        this.searchId = 0;
        this.duration = 1440;

        } catch (error) {
            console.log(error)
        }
    }
    addStatusMessage(message){
        if (Fluidity.Refinery.vue.statuses.length > 10) {
            Fluidity.Refinery.vue.statuses.pop()
        }
        Fluidity.Refinery.vue.statuses.unshift(new Date().toLocaleTimeString() + ": " + message);
    }

    bind() {
        let self = this;
         $('#searchInputForm input').on('focusin', function(arg){
                    $(arg.target.parentElement).addClass("big-input");
                });

        $.Topic(Fluidity.Refinery.Topics.submitReply).subscribe(function(reply) {
            console.log("Status results:" + reply);
            Fluidity.Refinery.vue.statusMessage="Complete - status:" + reply;
            self.addStatusMessage("Model Construction Complete: " + reply)
            self.refreshModel()
        })

        $.Topic(Fluidity.Refinery.Topics.statusReply).subscribe(function(reply) {
            console.log("Status results:" + reply)
        })
        $.Topic(Fluidity.Refinery.Topics.modelReply).subscribe(function(reply) {
            console.log("Model Reply results:" + reply)
             Fluidity.Refinery.vue.modelDataUpdateStatus = 'Updated @' + new Date().toLocaleTimeString();
             Fluidity.Refinery.vue.modelTable.items.length = 0;
             reply.forEach(item => {
                Fluidity.Refinery.vue.modelTable.items.push({ name: item.name, modified: new Date(parseInt(item.modified)).toLocaleString(), size: '128k'} )
             })
        })

    }
    getTagValue(tagsInputItem) {
        if (tagsInputItem.length == 0) {
            return '*';
        } else {
            return tagsInputItem[0].text;
        }
    }

    refreshModel() {
        Fluidity.Refinery.vue.modelDataUpdateStatus = 'Refreshing model list'
        $.Topic(Fluidity.Refinery.Topics.model).publish(this.uuid.valueOf(), Fluidity.Refinery.vue.modelName);

    }
    submit() {
        this.uuid = new Fluidity.Util.UUID();
        console.log("Building search and submitting")
         let search = {
                    origin: Fluidity.username,
                    uid: this.uuid.valueOf(),
                    expression: this.getTagValue(Fluidity.Refinery.vue.input.bucket.tags) + '|' + this.getTagValue(Fluidity.Refinery.vue.input.filename.tags)
                        + '|' + this.getTagValue(Fluidity.Refinery.vue.input.filter.tags)  + '|' +  this.getTagValue(Fluidity.Refinery.vue.input.field.tags) + '|*|*|*',
                    from: new Date().getTime() - this.duration * 60 * 1000,
                    to: new Date().getTime()
                }
        this.searchId = search.uid;
        this.searchRequest = search;
        this.startTime =  new Date();

        this.addStatusMessage("Starting model query");

        Fluidity.Refinery.vue.statusMessage="Running:" + search.expression;
        $.Topic(Fluidity.Refinery.Topics.submit).publish(search, Fluidity.Refinery.vue.modelNameInput.name);
    }
}