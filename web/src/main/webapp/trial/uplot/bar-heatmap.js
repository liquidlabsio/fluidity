
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


            let decimal = ladderY - parseInt(ladderY)

            let colorHeat = 255 * decimal;
           this.u.ctx.fillStyle = "rgba(51,154," + colorHeat + ",1)"

            console.log("GOT:" + ladderAsY + " to " + this.bodyX)

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
            let ladderY         = u.data[3][i];

            ladderY.forEach(renderLadderY.bind({ u:u, xVal:xVal, bodyX:bodyX, bodyWidth:bodyWidth}));
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
    // min
    [1, 1 , 1, 1, 1],
    // max
    [100, 100,100,100,100],

    //  ladders
    [
    [10.1,12.3,14.8,18.9,30.15, 50.10, 43.2],
    [10.1,12.3,14.8,18.9,30.15, 50.10, 43.2, 60.1, 70.5, 63.9],
    [70.5, 90.10, 13.2, 10.1,12.3,14.8,18.9,30.15, 50.10, 43.2],
    [70.5, 90.10, 13.2],
    [18.9,30.15, 50.10, 70.5, 90.10, 13.2]]
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
        ],
    scales: {
        y: {
          auto: true
        },
    }
};

let u = new uPlot(opts, data, document.body);