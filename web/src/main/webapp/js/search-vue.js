 searchChart = new Vue({
        el: '#chart',
        components: {
          apexchart: VueApexCharts,
        },
         methods: {
              doit: function (event, chartContext, config) {
                let point = searchChart.series[config.seriesIndex].data[config.dataPointIndex];
                console.log("series:" + searchChart.series[config.seriesIndex].name)
                console.log("time:" + point[0])
                console.log("value:" + point[1])
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
            }
            },
        }

})




var searchStats = new Vue({
  el: '#searchResultsChart',
  data: {
    stats: 'Stats[]'
  }
})


var searchInput = new Vue({
    el: '#searchInput',
    data: {
        searchExpression: ''
    }
})
var searchSubmit = new Vue({
    el: '#searchSubmit',
    methods: {
      searchSubmit: function () {
        let search = {
            origin: 'username',
            uid: new Date().getTime(),
            expression: searchInput.searchExpression,
            from: new Date().getTime() - searcher.duration * 60 * 1000,
            to: new Date().getTime()
        }
        searcher.submitSearch(search)
      }
  }
})