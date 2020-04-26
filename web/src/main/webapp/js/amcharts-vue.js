// simple code pen example: https://codepen.io/hossein_vatankhah/pen/gNQPKr
// vue component reference to follow
// 1. - apex chart guide: https://apexcharts.com/docs/vue-charts/
// 2. - https://github.com/apexcharts/vue-apexcharts/blob/master/src/ApexCharts.component.js
// 3. - https://github.com/apexcharts/vue-apexcharts
/**
Try to support data format
*   [
 *     {
 *       name: "Series 1",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     },
 *      {
 *       name: "Series 2",
 *       data: [
 *         [1486684800000, 34],
 *         [1486771200000, 43],
 *         [1486857600000, 31] ,
 *         [1486944000000, 43],
 *         [1487030400000, 33],
 *         [1487116800000, 52]
 *       ]
 *     }
 *   ]
 **/
standardChartData = [
        {name: 'Series 1',
            data: [ [1486684800000, 34], [1486771200000, 43],[1486857600000, 31] , [1486944000000, 43],  [1487030400000, 33], [1487116800000, 52] ]
         },
      { name: 'Series 2',
      data: [[1486684800000, 34],  [1486771200000, 43], [1486857600000, 31],  [1486944000000, 43], [1487030400000, 33],[1487116800000, 52] ]
      }];

function getChartByContainerRef(container) {
  var charts = am4core.registry.baseSprites[i];
  for(var i = 0; i < charts.length; i++) {
    if (charts[i].svgContainer === container) {
      return charts[i];
    }
  }
}

function getChartByContainerId(id) {
  var charts = am4core.registry.baseSprites[i];
  for(var i = 0; i < charts.length; i++) {
    if (charts[i].svgContainer.id == id) {
      return charts[i];
    }
  }
}
Vue.component("amchart-xy",{
    props: ['type', 'series', 'options', 'width', 'height'],
    template:
            '<div class="amchart-xy" ref="chartdiv"></div>',
  created() {
        console.log("CREATING AM CHARTS type:" + this.chartType)
        this.type = 'line';
        
    },
    mounted() {
        console.log("MOUNT AM CHARTS type:" + this.chartType);

        if (this.chart == null) {
        console.log("CREATEEEEE" + this.chartType);

        } else {
        console.log("ALREADY EXISTS")
         console.log(chart)
         this.refresh()
        }

        let chart = am4core.create(this.$refs.chartdiv, am4charts.XYChart);

        console.log("DONE MOUNT AM CHARTS")

        chart.paddingRight = 20;

        chart.data = this.getData();

        let dateAxis = chart.xAxes.push(new am4charts.DateAxis());
        dateAxis.renderer.grid.template.location = 0;

        let valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
        valueAxis.tooltip.disabled = true;
        valueAxis.renderer.minWidth = 35;

        let series = chart.series.push(this.getSeriesType());
        series.dataFields.dateX = "date";
        series.dataFields.valueY = "value";

        series.tooltipText = "{valueY.value}";
        chart.cursor = new am4charts.XYCursor();

        let scrollbarX = new am4charts.XYChartScrollbar();
        scrollbarX.series.push(series);
        chart.scrollbarX = scrollbarX;

        this.chart = chart;
      },

    beforeDestroy() {
        if (this.chart) {
          this.chart.dispose();
        }
      },
  methods: {
    init() {
        console.log("init");
    },
    refresh() {
        console.log("refresh");
        this.destroy();
        return this.init();
    },
    destroy() {
        console.log("destroy");
        this.chart.destroy();
    },
    updateSeries(newSeries, animate) {
        console.log("updateSeries");
        return this.chart.updateSeries(newSeries, animate);
    },
    updateOptions(newOptions, redrawPaths, animate, updateSyncedCharts) {
        return this.chart.updateOptions(
               newOptions,
               redrawPaths,
               animate,
               updateSyncedCharts
             );
    },
     getData() {
//         console.log("getData")
//            let data = [];
//            let visits = 10;
//            for (let i = 1; i < 366; i++) {
//            visits += Math.round((Math.random() < 0.5 ? 1 : -1) * Math.random() * 10);
//            data.push({ date: new Date(2018, 0, i), name: "name" + i, value: visits });
//            }
//            return data;
        return standardChartData;
        }
        ,
      getSeriesType() {
       console.log("getSeriesType:" + this.chartType)
        return new am4charts.LineSeries()
      }

   }
})


searchChart = new Vue({
  el: '#searchChartDiv',
  data: function() {
     return {
       options: {
         chart: {
           id: 'vuechart-example'
         },
         xaxis: {
           categories: [1991, 1992, 1993, 1994, 1995, 1996, 1997, 1998]
         }
       },
       series:  []
     }
   }
});



