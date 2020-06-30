class DataflowsForTime {

    load(data, element) {
        this.element = element;
        this.data = data;
        google.charts.load('current', {'packages':['bar']});
        google.charts.setOnLoadCallback(this.drawChart.bind({ data: data, element: element }));
    }

    drawChart() {
        let ddata = google.visualization.arrayToDataTable(this.data);
        var options = {

          isStacked: true,
          bars: 'horizontal', // Required for Material Bar Charts.
          'width':'100%',
          'height':'100%',
          "chartArea": {
                  "width":'100%',
                  "height":'100%'
              }
        };

        this.chart = new google.charts.Bar(this.element);
        this.chart.draw(ddata, google.charts.Bar.convertOptions(options));
        google.visualization.events.addListener(this.chart, 'click', function() {
          //table.setSelection(orgchart.getSelection());
          console.log("Clicked thingy:" + event)
        });
    }

    click(event) {
        console.log("Doing a Click")
    }
}