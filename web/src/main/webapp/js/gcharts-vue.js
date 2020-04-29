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
/**
* type: line, bar, 
**/
    props: ["type", "series", "options", "width", "height"],
    template:
            "<div class='gchart-xy' ref='chartdiv'></div>",
    created() {
        this.$watch("options", (options) => {
          if (!this.chart && options) {
           console.log("Chart options changed");
          }
        });

        this.$watch("series", (series) => {
            console.log("Chart series changed");
            this.updateSeries(this.series);
        });
        this.$watch("type", (type) => {
            console.log("Chart type changed");
             google.charts.load('current', {'packages':[this.type]});
             google.charts.setOnLoadCallback(this.draw);
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
          console.log("CREATE:" + this.type);
          } else {
          console.log("ALREADY EXISTS")
           console.log(chart);
           this.refresh();
          }

          google.charts.load("current", {"packages":[this.type]});
//          google.charts.load('current', {'packages':['corechart']});
          //google.charts.setOnLoadCallback(this.drawChart);
        },
  methods: {
    init() {
        console.log("init");
    },
     updateSeries(newSeries) {
        if (newSeries.length == 0) {
        console.log("Empty series");
        return;
        }
        let data = new google.visualization.DataTable();
        data.addColumn("datetime", "Time");
        var rows = []
        newSeries.forEach((element, index) => {
            data.addColumn("number", element.name);
            element.data.forEach((element, index) => {
                var row = [];
                if (rows.length <= index) {
                    row.push(new Date(element[0]));
                    row.push(element[1]);
                    rows.push(row);
                } else {
                    row = rows[index];
                    row.push(element[1]);
                }
            })
        })

        data.addRows(rows);
        this.data = data;
        this.setType(this.type);
    },
    refresh() {
        console.log("refresh");
        setType(this.type);
//        this.destroy();
//        return this.init();
    },
    destroy() {
        console.log("destroy");
        this.chart.destroy();
    },
    draw() {
        this.setType(this.type);
    },
    setType(type) {
//               // classic google charts
              var classicOptions = {
                    height: this.height,
                       explorer: {
                         },
                      // colors: ['#D44E41'],
                     };
             // material google charts
        var materialOptions = {
            explorer: {},
             height: this.height,
            //width: 600,
            curveType: 'function',
            legend: { position: 'bottom', maxLines: 3 },
          };

        if (type == 'line') {
            options.curveType = 'function';
            this.chart = new google.charts.Line(this.$refs.chartdiv);
            this.chart.draw(this.data, google.charts.Line.convertOptions(materialOptions));
//            this.chart = new google.visualization.LineChart(this.$refs.chartdiv);
//            this.chart.draw(this.data, classic);
        } else if (type == 'bar')  {
            this.chart = new google.charts.Bar(this.$refs.chartdiv);
            options.bar =  { groupWidth: '90%' };
           // options.isStacked = true;
            this.chart.draw(this.data, google.charts.Bar.convertOptions(options));
        } else {
           console.log("Invalid chart Type:" + type)
        }
    }
   }
})
//searchChart = new Vue({
//  el: '#searchChart',
//  data: {
//       type: 'line',
//       options: {
//         chart: {
//           id: 'vuechart-example'
//         },
//       },
//       series:  standardChartData
//   }
//});



