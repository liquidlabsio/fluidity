/**
* Vue component for the search page
**/
var toggle = Vue.component("vue-toggle",{
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

searchTimeSeriesToggle = new Vue({
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


searchInputBucket = new Vue({
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
        'tags.contains(TEXT)'
      ]
    }
  }
})
searchInputFilename = new Vue({
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
        'filename.equals(TEXT)'
      ]
    }
  }
})
searchInputRecord = new Vue({
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
searchInputField = new Vue({
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


searchInputAnalytic = new Vue({
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

var searchSubmit = new Vue({
    el: '#searchSubmit',
    methods: {
      searchSubmit: function () {
        let search = {
            origin: 'username',
            uid: new Date().getTime(),
            expression: searchInputBucket.query + '|' + searchInputFilename.query + '|' + searchInputRecord.query + '|' +  searchInputField.query + '|' + searchInputAnalytic.query + '|' + searchTimeSeriesToggle.time,
            from: new Date().getTime() - searcher.duration * 60 * 1000,
            to: new Date().getTime()
        }
        searcher.submitSearch(search)
      }
  }
})

var searchStats = new Vue({
  el: '#searchResultStats',
  data: {
    stats: 'Stats[]'
  }
})
var searchFileToOpenInfo = new Vue({
  el: '#searchFileInfo',
  data: {
    searchFileInfo: '---'
  }
})

