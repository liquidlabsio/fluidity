Fluidity.Search.searchChart = new Vue({
        el: '#searchChart',
        components: {
          apexchart: VueApexCharts,
        },
         methods: {
              click(event, chartContext, config) {
                if (config.seriesIndex == -1) return false;
                let point = Fluidity.Search.searchChart.series[config.seriesIndex].data[config.dataPointIndex];
//                console.log("series:" + searchChart.series[config.seriesIndex].name)
//                console.log("time:" + point[0])
//                console.log("value:" + point[1])
                 Fluidity.Search.searchStats.stats = "Getting events from:" + new Date(point[0]).toLocaleString();
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
                          type: style,
                          stacked: true
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
                    type: "line",
                    animations: {
                          enabled: false,
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
                                       Fluidity.Search.searchChart.setChartStyle('bar')
                                    }
                                },
                                {
                                    icon: '<img class="apex-toolbar-icon" src="img/tools-line.png" width="25">',
                                    index: 2,
                                    title: 'Render as line-chart',
                                    class: 'custom-icon',
                                    click: function (chart, options, e) {
                                       Fluidity.Search.searchChart.setChartStyle('line')
                                    }
                                },
                                 {
                                        icon: '<img class="apex-toolbar-icon" src="img/tools-area.png" width="25">',
                                        index: 2,
                                        title: 'Render as area',
                                        class: 'custom-icon',
                                        click: function (chart, options, e) {
                                           Fluidity.Search.searchChart.setChartStyle('area')
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
                    rotate: -90,
                    datetimeUTC: false,
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



