let flowData = [
                                 ['Correlation', 'Register', 'Process', 'Load','Complete', 'Acknowledge'],
                                 ['txn-1000', 1000, 400, 200,1000, 400],
                                 ['txn-1222', 1170, 460, 250, 1000, 400],
                                 ['txn-133300', 660, 1120, 300, 1000, 400],
                                 ['txn-101gesr00', 1030, 540, 350, 1000, 400],
                                 ['txn-2000', 1000, 400, 200,1000, 400],
                                 ['txn-3222', 1170, 460, 250, 1000, 400],
                                 ['txn-433300', 660, 1120, 300, 1000, 400],
                                 ['txn-401gesr00', 1030, 540, 350, 1000, 400],
                                 ['txn-4000', 1000, 400, 200,1000, 400],
                                 ['txn-4222', 1170, 460, 250, 1000, 400],
                                 ['txn-533300', 660, 1120, 300, 1000, 400],
                                 ['txn-601gesr00', 1030, 540, 350, 1000, 400],
                                 ['txn-7000', 1000, 400, 200,1000, 400],
                                 ['txn-8222', 1170, 460, 250, 1000, 400],
                                 ['txn-933300', 660, 1120, 300, 1000, 400],
                                 ['txn-001gesr00', 1030, 540, 350, 1000, 400]
                               ];

class DataflowsForTime {

    load(element, rest) {
        this.element = element;
        this.rest = rest;
        this.loadGChart();
    }
    loadGChart() {
        google.charts.load('current', {'packages':['corechart']});
        google.charts.setOnLoadCallback(this.loaded.bind({ self: this }));
    }
    loaded() {
        var options = {
          isStacked: true,
          bars: 'horizontal', // Required for Material Bar Charts.
          width:'100%',
          height:'100%',
          isStacked: true,
          theme: 'material'
        };

        self = this.self;

        self.options = options;
        self.chart = new google.visualization.BarChart(self.element);
        google.visualization.events.addListener(self.chart, 'click', function() {
          //table.setSelection(orgchart.getSelection());
          console.log("Clicked to drill-down:" + event)
        });
    }

    setData(self, data) {
        let ddata = google.visualization.arrayToDataTable(data);
        self.chart.draw(ddata, self.options);
    }

    // load data into this view
    click(self, index, timeX, valueY) {
        console.log("Loading data for Index:" + index + " X:" + timeX + " Y:" + valueY)
        // rest - callback with data
       // this.rest.dataflowsForTime(Fluidity.Dataflow.vue.modelNameInput.name, timeX, valueY, self.loadData);
       self.setData(self, flowData);
    }
}