// simple code pen example: https://codepen.io/hossein_vatankhah/pen/gNQPKr
// vue component reference to follow
// 1. - apex chart guide: https://apexcharts.com/docs/vue-charts/
// 2. -
// 3. - https://github.com/apexcharts/vue-apexcharts
standardChartData = [
        {name: 'Series 1',
            data: [ [1486684800000, 34], [1486771200000, 43],[1486857600000, 31] , [1486944000000, 43],  [1487030400000, 33], [1487116800000, 52] ]
         },
      { name: 'Series 2',
      data: [[1486684800000, 34],  [1486771200000, 43], [1486857600000, 31],  [1486944000000, 43], [1487030400000, 33],[1487116800000, 52] ]
      }];


Vue.component("gchart-xy",{
    props: ['type', 'series', 'options', 'width', 'height'],
    template:
            '<div class="gchart-xy" ref="chartdiv"></div>',
  created() {
        console.log("CREATING GCHARTS type:" + this.chartType)
        this.type = 'line';
        
    },
    created() {
        this.$watch("options", options => {
          if (!this.chart && options) {
           console.log("Chart options changed")
          }
        });

        this.$watch("series", series => {
         console.log("Chart series changed")
//          if (!this.chart && series) {
//            this.init();
//          } else {
            this.updateSeries(this.series);
//          }
        });
      },
    beforeDestroy() {
        if (this.chart) {
          this.chart.dispose();
        }
      },
    mounted() {
          console.log("MOUNT GCHARTS type:" + this.type);

          if (this.chart == null) {
          console.log("CREATEEEEE" + this.type);

          } else {
          console.log("ALREADY EXISTS")
           console.log(chart)
           this.refresh()
          }

          google.charts.load('current', {'packages':[this.type]});
          //google.charts.setOnLoadCallback(this.drawChart);
        },
  methods: {
    init() {
        console.log("init");
    },
     updateSeries(newSeries) {
                var data = new google.visualization.DataTable();
                data.addColumn('datetime', 'Time');
                var rows = []
                newSeries.forEach((element, index) => {
                    data.addColumn('number', element.name);
                    element.data.forEach((element, index) => {
                        if (rows.length <= index) {
                            row = [];
                            row.push(new Date(element[0]));
                            row.push(element[1]);
                            rows.push(row);
                        } else {
                            row = rows[index];
                            row.push(element[1])
                        }
                    })
                })

                data.addRows(rows);
                var options = {
                  chart: {
//                    title: 'Box Office Earnings in First Two Weeks of Opening',
//                    subtitle: 'in millions of dollars (USD)'
                  }
                };

                this.chart = new google.charts.Line(this.$refs.chartdiv);
                this.chart.draw(data, google.charts.Line.convertOptions(options));
              },
    refresh() {
        console.log("refresh");
        this.destroy();
        return this.init();
    },
    destroy() {
        console.log("destroy");
        this.chart.destroy();
    }
   }
})


searchChart = new Vue({
  el: '#searchChartDiv',
  data: {
       options: {
         chart: {
           id: 'vuechart-example'
         },
       },
       series:  standardChartData
   }
});



