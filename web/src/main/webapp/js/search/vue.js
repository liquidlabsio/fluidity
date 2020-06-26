/**
* Vue component for the search page
**/
Fluidity.Search.toggle = Vue.component("vue-toggle",{
  props: ['values','selected','default'],
  created: function () {
      this.selected = this.default;
    },
    methods: {
      changeSelectVal: function(val) {
        this.selected = val;
        // no idea why vue data binding isnt working.... moving on...fix later
        searchTimeSeriesToggle.time = val;
      }
    },
    template:
        '<fieldset class="form-group" style="margin: 0px;">'+
                                  '<div class="form-group" style="margin: 0px;">'+
                '<div class="btn-group">'+
                            '<button type="button"'+
                                    'v-for="(key, val) in values"'+
                                    '@click="changeSelectVal(key)"'+
                                    ':class="[\'btn\', { \'btn-outline-primary\': selected === key, \'btn-outline-secondary\': selected !== key }]"'+
                            '>{{ val }}</button>'+
                '</div>' +
         '</div></fieldset>'
})

Fluidity.Search.searchTimeSeriesToggle = new Vue({
  el: '#searchTimeSeriesSelection',
  data() {
    return {
      time: 'time.series()',
      styles: {
        'TimeSeries': 'time.series()',
        'TimeOverlay': 'time.overlay()',
      }
    }
  }
});


Fluidity.Search.searchInputBucket = new Vue({
  el: '#bucketFilterWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'bucket.equals(BUCKET_NAME)',
        'bucket.contains(TEXT_PART)',
        'tags.contains(TEXT)',
        'tags.equals(TEXT)'
      ]
    }
  }
})
Fluidity.Search.searchInputFilename = new Vue({
  el: '#fileFilterWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'filename.contains(TEXT)',
        'somefilename.log'
      ]
    }
  }
})
Fluidity.Search.searchInputRecord = new Vue({
  el: '#recordFilterWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'record.contains(TEXT)',
        'record.matches(.*TXN:.*)',
        'findThis AND alsoThis',
        'foundThis OR foundThat'
      ]
    }
  }
})
Fluidity.Search.searchInputField = new Vue({
  el: '#fieldExtractWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'field.getKVPair(TEXT_NAME:)',
        'field.getJsonPair(TEXT_NAME)',
        'field.groupPair(.*[TXN]:[%d].*)',
        'field.groups(.*[TXN]:.*)'
      ]
    }
  }
})


Fluidity.Search.searchInputAnalytic = new Vue({
  el: '#analyticExtractWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'analytic.count()',
        'analytic.countEach()',
        'analytic.countDistinct()',
        'analytic.countBy(tag)',
        'analytic.stats()',
        'analytic.statsBy(tag)',
        'analytic.statsBy(field1)'
      ]
    }
  }
})

Fluidity.Search.searchInputGroupBy = new Vue({
  el: '#groupByWrapper',
  components: {
    VueBootstrapTypeahead
  },
  data() {
    return {
      query: '*',
      completes: [
        '*',
        'groupBy(tag)',
        'groupBy(path)',
        'groupBy(path[1])',
        'groupBy(path[1-2])',
        'groupBy(path[last])',
      ]
    }
  }
})

Fluidity.Search.searchSubmit = new Vue({
    el: '#searchSubmit',
    methods: {
      searchSubmit: function () {
        let search = {
            origin: Fluidity.username,
            uid: new Date().getTime(),
            expression: Fluidity.Search.searchInputBucket.query + '|' + Fluidity.Search.searchInputFilename.query + '|' + Fluidity.Search.searchInputRecord.query
                        + '|' +  Fluidity.Search.searchInputField.query + '|' + Fluidity.Search.searchInputAnalytic.query
                        + '|' + Fluidity.Search.searchTimeSeriesToggle.time + '|' + Fluidity.Search.searchInputGroupBy.query ,
            from: new Date().getTime() - searcher.duration * 60 * 1000,
            to: new Date().getTime()
        }
        searcher.submitSearch(search)
      }
  }
})

Fluidity.Search.searchStats = new Vue({
  el: '#searchResultStats',
  data: {
    stats: 'Stats[]'
  }
})
Fluidity.Search.searchFileToOpenInfo = new Vue({
  el: '#searchFileInfo',
  data: {
    searchFileInfo: '---'
  }
})

