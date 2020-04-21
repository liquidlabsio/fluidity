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
  data: {
        time: 'time.series()',
        styles: {
          'TimeSeries': 'time.series()',
          'TimeOverlay': 'time.overlay()',
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
        'analytic.countDistinct()',
        'analytic.countBy(tag)',
        'analytic.stats()',
        'analytic.statsBy(tag)',
        'analytic.statsBy(field1)'
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
              click(event, chartContext, config) {
                if (config.seriesIndex == -1) return false;
                let point = searchChart.series[config.seriesIndex].data[config.dataPointIndex];
//                console.log("series:" + searchChart.series[config.seriesIndex].name)
//                console.log("time:" + point[0])
//                console.log("value:" + point[1])
                 searchStats.stats = "Getting events from:" + new Date(point[0]).toLocaleString();
                 searcher.startTime = new Date();
                $.Topic(Fluidity.Search.Topics.getFinalEvents).publish(searcher.searchRequest, point[0]);

              },
              updateTheme(e) {
                this.chartOptions = {
                  theme: {
                    palette: e.target.value
                  }
                };
              },
               setChartStyle(style) {
                              this.chartOptions = {
                                chart: {
                                  type: style
                                }
                              };
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
                    height: 150,
                    type: 'bar',
                    animations: {
                                 enabled: false
                    },
                     toolbar: {
                        show: true,
                        tools: {
                                  download: true,
                                  selection: true,
                                  zoom: true,
                                  zoomin: false,
                                  zoomout: false,
                                  pan: true,
                            customIcons: [
                                {
                                    icon: '<img class="apex-toolbar-icon" src="img/tools-bar.png" width="25">',
                                    index: 1,
                                    title: 'Render as bar-chart',
                                    class: 'custom-icon',
                                    click: function (chart, options, e) {
                                       searchChart.setChartStyle('bar')
                                    }
                                },
                                {
                                    icon: '<img class="apex-toolbar-icon" src="img/tools-line.png" width="25">',
                                    index: 2,
                                    title: 'Render as line-chart',
                                    class: 'custom-icon',
                                    click: function (chart, options, e) {
                                       searchChart.setChartStyle('line')
                                    }
                                },
                                 {
                                        icon: '<img class="apex-toolbar-icon" src="img/tools-area.png" width="25">',
                                        index: 2,
                                        title: 'Render as area',
                                        class: 'custom-icon',
                                        click: function (chart, options, e) {
                                           searchChart.setChartStyle('area')
                                       }
                                    }
                            ]
                        }
                    }
               },
                dataLabels: {
                             enabled: false,
                           },
                stroke: {
                         width: 1,
                         curve: 'smooth'
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
                    format: "HH:mm dd-MMM"
                    }
                },

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

