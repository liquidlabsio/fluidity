
Fluidity.Refinery.vue = new Vue({
    el: '#refinery',
    data() {
      return {
            alertMessage: "",
            alertVisible: false,
            input: {
                bucket: {
                    tag:'',
                    tags: [ { text: '*' } ],
                    autocompleteItems: [{
                            text: '*',
                          }, {
                            text: 'bucket.equals(BUCKET_NAME)',
                          }, {
                            text: 'bucket.contains(TEXT_PATH)',
                          }, {
                            text: 'tags.contains(TEXT)',
                          }]
                },
                filename: {
                    tag:'',
                    tags: [ { text: '*' } ],
                    autocompleteItems: [{
                            text: '*',
                          }, {
                            text: 'filename.contains(TEXT)',
                          }, {
                            text: 'filename.equals(TEXT)',
                     }]
                },
                filter: {
                    tag:'',
                    tags: [ { text: '*' } ],
                    autocompleteItems: [{
                            text: '*',
                          }, {
                          text: 'record.contains(TEXT)',
                          },{
                            text: 'someTextThatMatches',
                            },{
                            text: 'findThis AND alsoThis',
                            },{
                            text: 'foundThis OR foundThat'
                      }]
                },
                 field: {
                    tag:'',
                    tags: [ { text: '*' } ],
                    autocompleteItems: [{
                        text: '*',
                      }, {
                        text: 'field.getJsonPair(JsonName)',
                      }, {
                        text: 'field.getKVPair(TextKey)',
                }]
                },
            },

            modelInterval: 1,
            statuses:[ '-' ],
            statusMessage: '...',
            modelDataUpdateStatus: '',
            modelTable: {
                items:  [ { name: '-----', modified: '-----', size: '---'} ],
               fields: [ 'name', 'modified', 'size' ]
               },
            modelNameInput: {
                name: "",
                names: []
            },

        }
    },
    computed: {
        inputBucketFilteredItems() {
          return this.input.bucket.autocompleteItems.filter(i => {
            return i.text.toLowerCase().indexOf(this.input.bucket.tag.toLowerCase()) !== -1;
          });
        },
        inputFilenameFilteredItems() {
          return this.input.filename.autocompleteItems.filter(i => {
            return i.text.toLowerCase().indexOf(this.input.filename.tag.toLowerCase()) !== -1;
          });
        },
        inputFilterFilteredItems() {
          return this.input.filter.autocompleteItems.filter(i => {
            return i.text.toLowerCase().indexOf(this.input.filter.tag.toLowerCase()) !== -1;
          });
        },
        inputFieldFilteredItems() {
          return this.input.field.autocompleteItems.filter(i => {
            return i.text.toLowerCase().indexOf(this.input.field.tag.toLowerCase()) !== -1;
          });
        }


    },
    watch: {
      show(newVal) {
        console.log('Alert is now ' + (newVal ? 'visible' : 'hidden'))
      }
    },
    mounted() {
        //this.listModels();
      },
    methods: {
       refinerySubmit() {
        console.log('Submit model build')
        Fluidity.Refinery.refinery.submit()
      },
      loadHistogramData(therow) {
        console.log('Load histogram data search:' + therow);
        $.Topic(Fluidity.Explorer.Topics.setExplorerToOpen).publish([ "storage://" + therow[0].name, 0, 0 ])
      },
      refreshModel() {
        Fluidity.Refinery.refinery.refreshModel()
      },

      fetchModels() {
      			return fetch(SERVICE_URL + '/dataflow/model/list?'
      			    + new URLSearchParams({ tenant: DEFAULT_TENANT })
      			 )
      			.then(res => res.json())
      			 .then(res => {
      			    console.log("Got:" + res);
      			    return res;
      			 });
      		},
      loadModel(name) {
            return fetch(SERVICE_URL + '/dataflow/model/load?'
                + new URLSearchParams(
                        { tenant: DEFAULT_TENANT, model: name})
                )
                .then(res => res.json())
                .then(res => {
                    let expr = res.query.split('|');
                    Fluidity.Refinery.vue.input.bucket.tag = ""
                    Fluidity.Refinery.vue.input.bucket.tags = [ { text: expr[0] } ];

                    Fluidity.Refinery.vue.input.filename.tag = ""
                    Fluidity.Refinery.vue.input.filename.tags =  [ { text: expr[1] } ];

                    Fluidity.Refinery.vue.input.filter.tag = ""
                    Fluidity.Refinery.vue.input.filter.tags =  [ { text: expr[2] } ];

                    Fluidity.Refinery.vue.input.field.tag = ""
                    Fluidity.Refinery.vue.input.field.tags =  [ { text: expr[3] } ];

                    $.Topic(Fluidity.Refinery.Topics.model).publish("uid-123", Fluidity.Refinery.vue.modelNameInput.name);
                });
            },
      saveModel() {
                let query = Fluidity.Refinery.vue.input.bucket.tags[0].text + '|'
                            + Fluidity.Refinery.vue.input.filename.tags[0].text + '|'
                            + Fluidity.Refinery.vue.input.filter.tags[0].text + '|'
                            + Fluidity.Refinery.vue.input.field.tags[0].text;
                let modelData = {
                        name: Fluidity.Refinery.vue.modelNameInput.name,
                        query: query,
                        schedule: 1
                };

      			fetch(SERVICE_URL + '/dataflow/model/save?'
          			+ new URLSearchParams({
                            tenant: DEFAULT_TENANT,
                            model: Fluidity.Refinery.vue.modelNameInput.name,
                            data: JSON.stringify(modelData),
                        })
      			)
      			.then(res => res.json())
      			.then(res => {
      			    Fluidity.Refinery.vue.alertVisible = true;
      			    Fluidity.Refinery.vue.alertMessage = "Model was saved:" + res;
      			    setTimeout(function(){ Fluidity.Refinery.vue.alertVisible = false; }, 3000);

      			});
      		},


    }
})