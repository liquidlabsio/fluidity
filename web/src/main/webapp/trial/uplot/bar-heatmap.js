
// column-highlights the hovered x index
function columnHighlightPlugin({ className, style = {backgroundColor: "rgba(51,204,255,0.3)"} } = {}) {
    let underEl, overEl, highlightEl, currIdx;

    function init(u) {
        underEl = u.root.querySelector(".under");
        overEl = u.root.querySelector(".over");

        highlightEl = document.createElement("div");

        className && highlightEl.classList.add(className);

        uPlot.assign(highlightEl.style, {
            pointerEvents: "none",
            display: "none",
            position: "absolute",
            left: 0,
            top: 0,
            height: "100%",
            ...style
        });

        underEl.appendChild(highlightEl);

        // show/hide highlight on enter/exit
        overEl.addEventListener("mouseenter", () => {highlightEl.style.display = null;});
        overEl.addEventListener("mouseleave", () => {highlightEl.style.display = "none";});
    }

    function update(u) {
        if (currIdx !== u.cursor.idx) {
            currIdx = u.cursor.idx;

            let [iMin, iMax] = u.series[0].idxs;

            const dx    = iMax - iMin;
            const width = (u.bbox.width / dx) / devicePixelRatio;
            const xVal  = u.scales.x.distr == 2 ? currIdx : u.data[0][currIdx];
            const left  = u.valToPos(xVal, "x") - width / 2;

            highlightEl.style.transform = "translateX(" + Math.round(left) + "px)";
            highlightEl.style.width = Math.round(width) + "px";
        }
    }

    return {
        opts: (u, opts) => {
            uPlot.assign(opts, {
                cursor: {
                    x: false,
                    y: false,
                }
            });
        },
        hooks: {
            init: init,
            setCursor: update,
        }
    };
}


function ladderRenderPlugin({ gap = 2, shadowColor = "#000000", bodyMaxWidth = 50 } = {}) {

    function renderLadderY(ladderY) {
            let ladderAsY = this.u.valToPos(ladderY,   "y", true);

            let ladderValue = ladderY - parseInt(ladderY)
            let ladderPercent = ladderValue * this.maxHeatValue;

            let colorHeat = parseInt(255 * ladderPercent/100.0);
           this.u.ctx.fillStyle = "rgba(50,50," + colorHeat + ",0.5)"

            this.u.ctx.fillRect(
                Math.round(this.bodyX),
                Math.round(ladderAsY),
                Math.round(this.bodyWidth),
                Math.round(5),
            );
    }

    function drawSeries(u) {
        u.ctx.save();

        let [iMin, iMax] = u.series[0].idxs;
        let columnWidth  = u.bbox.width / (iMax - iMin);
        let bodyWidth    = Math.min(bodyMaxWidth, columnWidth - gap);

        for (let i = iMin; i <= iMax; i++) {
            let xVal         = u.scales.x.distr == 2 ? i : u.data[0][i];
            let timeAsX      = u.valToPos(xVal,  "x", true);
            let bodyX        = timeAsX - (bodyWidth / 2);
            let ladderY         = u.data[1][i];

            ladderY.forEach(renderLadderY.bind({ u:u, xVal:xVal, bodyX:bodyX, bodyWidth:bodyWidth, maxHeatValue: u.data[2][1]}));
        }
        u.ctx.restore();
    }

    return {
        hooks: {
            draw: drawSeries,
        }
    };
}

const data = [
    // dates
    [1546300800,1546387200,1546473600,1546560000,1546819200],
    //  ladders
    [
        [80.0100, 50.0500, 43.0999],
        [10.0100,12.0300,14.0800,18.0900,30.0150, 50.0100, 43.0200, 60.0100, 70.05, 63.9],
        [70.5, 90.10, 13.2, 10.1,12.3,14.8,18.9,30.15, 50.10, 43.2],
        [70.5, 90.10, 13.2],
        [18.9,30.15, 50.10, 70.5, 90.10, 13.0999]

    ],
    // global heatmap min and max value
    [0,1000]
];

const fmtDate = uPlot.fmtDate("{YYYY}-{MM}-{DD} {h}:{mm}:{ss}");
const tzDate = ts => uPlot.tzDate(new Date(ts * 1e3), "Etc/UTC");

const opts = {
    width: 1440,
    height: 600,
    title: "Latency heatmap ladder",
    tzDate,
    plugins: [
       columnHighlightPlugin(),
       ladderRenderPlugin(),
    ],
      series: [
            {},
            {
              paths: () => null,
              points: {show: false},
            },
        ],
    scales: {
        y: {
            auto: false,
            range: u => {
                let [i0, i1] = u.series[0].idxs;

                let min = Infinity;
                let max = -Infinity;
                let heatmapMax = 0;

                // find min/max y values for all non-null values in shown series
                for (let i = i0; i <= i1; i++) {
                    let yVal = u.data[1][i];
                    if (yVal != null) {
                        for (let yy = 0; yy < yVal.length; yy++) {
                          min = Math.min(min, yVal[yy]);
                          max = Math.max(max, yVal[yy]);
                        }
                    }
                }
                return [min, max];
            },
        },
    }
};

let u = new uPlot(opts, data, document.body);