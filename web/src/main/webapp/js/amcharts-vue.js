// simple code pen example: https://codepen.io/hossein_vatankhah/pen/gNQPKr
var amChartsVue = Vue.component("amchart",{
//    props: ['values','selected','default'],
    template:
            '<div class="amchart" ref="chartdiv"></div>',

  created: function () {
      this.selected = this.default;
    },
    mounted: function() {
        console.log("CREATING AM CHARTS")

        let chart = am4core.create(this.$refs.chartdiv, am4charts.XYChart);

        console.log("DONE CREATING AM CHARTS")

        chart.paddingRight = 20;

        let data = [];
        let visits = 10;
        for (let i = 1; i < 366; i++) {
          visits += Math.round((Math.random() < 0.5 ? 1 : -1) * Math.random() * 10);
          data.push({ date: new Date(2018, 0, i), name: "name" + i, value: visits });
        }

        chart.data = data;

        let dateAxis = chart.xAxes.push(new am4charts.DateAxis());
        dateAxis.renderer.grid.template.location = 0;

        let valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
        valueAxis.tooltip.disabled = true;
        valueAxis.renderer.minWidth = 35;

        let series = chart.series.push(new am4charts.LineSeries());
        series.dataFields.dateX = "date";
        series.dataFields.valueY = "value";

        series.tooltipText = "{valueY.value}";
        chart.cursor = new am4charts.XYCursor();

        let scrollbarX = new am4charts.XYChartScrollbar();
        scrollbarX.series.push(series);
        chart.scrollbarX = scrollbarX;

        this.chart = chart;
      },

    beforeDestroy: function() {
        if (this.chart) {
          this.chart.dispose();
        }
      },
  methods: {
//      changeSelectVal: function(val) {
//        this.selected = val;
//        // no idea why vue data binding isnt working.... moving on...fix later
//      //  searchTimeSeriesToggle.time = val;
//      }
    }

})
amSearchChart = new Vue({
  el: '#searchChart',
  name: 'searchChart',
  data: function () {
    return {
      count: 0
    }
  }
});

//
//var app = new Vue({
//  el:"#app",
//  name: 'HelloWorld',
//  data() {
//  return {
//    msg:"amchart"
//  }
//},
//  mounted() {
//    let chart = am4core.create(this.$refs.chartdiv, am4charts.XYChart);
//    chart.paddingRight = 20;
//
//    let data = [];
//    let visits = 10;
//    for (let i = 1; i < 366; i++) {
//      visits += Math.round((Math.random() < 0.5 ? 1 : -1) * Math.random() * 10);
//      data.push({ date: new Date(2018, 0, i), name: "name" + i, value: visits });
//    }
//
//    chart.data = data;
//
//    let dateAxis = chart.xAxes.push(new am4charts.DateAxis());
//    dateAxis.renderer.grid.template.location = 0;
//
//    let valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
//    valueAxis.tooltip.disabled = true;
//    valueAxis.renderer.minWidth = 35;
//
//    let series = chart.series.push(new am4charts.LineSeries());
//    series.dataFields.dateX = "date";
//    series.dataFields.valueY = "value";
//
//    series.tooltipText = "{valueY.value}";
//    chart.cursor = new am4charts.XYCursor();
//
//    let scrollbarX = new am4charts.XYChartScrollbar();
//    scrollbarX.series.push(series);
//    chart.scrollbarX = scrollbarX;
//
//    this.chart = chart;
//  },
//
//  beforeDestroy() {
//    if (this.chart) {
//      this.chart.dispose();
//    }
//  }
//})
