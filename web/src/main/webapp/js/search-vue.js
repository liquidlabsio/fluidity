
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
        'record.matches(.*TXN:.*)'
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
        'analytic.countBy(tag)',
        'analytic.avg()',
        'analytic.avgEach()',
        'analytic.avgBy(tag)',
        'analytic.stats()',
        'analytic.statsEach()',
        'analytic.statsBy(tag)'
      ]
    }
  }
})


 searchChart = new Vue({
        el: '#searchChart',
        components: {
          apexchart: VueApexCharts,
        },
         methods: {
              click: function (event, chartContext, config) {
                let point = searchChart.series[config.seriesIndex].data[config.dataPointIndex];
//                console.log("series:" + searchChart.series[config.seriesIndex].name)
//                console.log("time:" + point[0])
//                console.log("value:" + point[1])
                 searchStats.stats = "Getting events from:" + new Date(point[0]).toLocaleString();
                 searcher.startTime = new Date();
                $.Topic(Precognito.Search.Topics.getFinalEvents).publish(searcher.searchRequest, point[0]);

              }
        },
        data: {
             series: [
                {
                  name: "no data",
                  data: [
                  ]
                }
              ]
            ,
            chartOptions: {
                chart: {
                  type: 'bar',
                  height: 350
                },
                dataLabels: {
                  enabled: false,
                },
                yaxis: {
                  title: {
                    text: 'Hits',
                  },
                  labels: {
                    formatter: function (y) {
                      return y.toFixed(0);
                    }
                  }
                },
                xaxis: {
                  type: 'datetime',
                  categories: [
                  ],
                  labels: {
                    rotate: -90
                  }
                },
                tooltip: {
                    x: {
                    format: "HH:MM dd-MMM"
                    }
                }
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
            expression: searchInputBucket.query + '|' + searchInputFilename.query + '|' + searchInputRecord.query + '|' +  searchInputField.query + '|' + searchInputAnalytic.query,
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

