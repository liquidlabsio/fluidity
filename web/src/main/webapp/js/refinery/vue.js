
Fluidity.Refinery.vue = new Vue({
    el: '#refinery',
    components: {
        VueBootstrapTypeahead
    },
    data() {
      return {
            input: {
                bucket: {
                    tag: '*',
                    tags: [],
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
                            tag: '*',
                            tags: [],
                            autocompleteItems: [{
                                    text: '*',
                                  }, {
                                    text: 'filename.contains(TEXT)',
                                  }, {
                                    text: 'filename.equals(TEXT)',
                             }]
                },
                filter: {
                            tag: '*',
                            tags: [],
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
                            tag: '*',
                            tags: [],
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
                name: "nothing",
                names: [
                    '*',
                    'bucket.equals(BUCKET_NAME)',
                    'bucket.contains(TEXT_PART)',
                    'tags.contains(TEXT)',
                    'tags.equals(TEXT)'
                  ]
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
        this.listModels();
      },
    methods: {
       refinerySubmit() {
        console.log('Submit search')
        Fluidity.Refinery.refinery.submit()
      },
      loadHistogramData(therow) {
        console.log('Load histogram data search:' + therow);
        $.Topic(Fluidity.Explorer.Topics.setExplorerToOpen).publish([ "storage://" + therow[0].name, 0, 0 ])
      },
      refreshModel() {
        Fluidity.Refinery.refinery.refreshModel()
      },
      listModels() {
      			fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(this.term)}&limit=10&media=music`)
      			.then(res => res.json())
      			.then(res => {
      			        Fluidity.Refinery.vue.modelNameInput.names = []
      			        res.results.forEach(result => {
                            Fluidity.Refinery.vue.modelNameInput.names.push("  " + result.artistName)
                        })
      			});
      		},
      loadModel() {
                fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(this.term)}&limit=10&media=music`)
                .then(res => res.json())
                .then(res => {
                    this.results = res.results;
               //     Fluidity.Refinery.vue.$refs.modelNameTypeAhead.inputValue = "Got results"
                    this.noResults = this.results.length === 0;
                });
            },
      saveModel() {
      			fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(this.term)}&limit=10&media=music`)
      			.then(res => res.json())
      			.then(res => {
      				this.results = res.results;
                 //   Fluidity.Refinery.vue.$refs.modelNameTypeAhead.inputValue = "Got results"
      				this.noResults = this.results.length === 0;
      			});
      		},


    }
})