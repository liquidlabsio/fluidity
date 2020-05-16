
Fluidity.Refinery.vue = new Vue({
    el: '#refinery',
    components: {
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
        modelName: 'Not Set',
        modelInterval: 1,
        statuses:[
                     '-'
                   ],
        statusMessage: '...',
        modelDataUpdateStatus: '',
        modelTable: {
            items:  [
                             { name: 'histo-12343-111.log', modified: '1/12/2020', size: '128k'},
                             { name: 'histo-12343-111.log', modified: '1/12/2020', size: '128k' },
                             { name: 'histo-12343-111.log', modified: '1/12/2020', size: '128k' },
                             { name: 'histo-12343-111.log', modified: '1/12/2020', size: '128k' },
                             { name: 'histo-12343-111.log', modified: '1/12/2020', size: '128k'},
                           ],
           fields: [
                     'name',
                     'modified',
                     'size'
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
    methods: {
       refinerySubmit() {
        console.log('Submit search')
        Fluidity.Refinery.refinery.submit()
      }
    }
})