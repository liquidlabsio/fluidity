

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

// converts the legend into a simple tooltip
function legendAsTooltipPlugin({ className, style = { backgroundColor:"black", color: "white" } } = {}) {
    let legendEl;

    function init(u, opts) {
        legendEl = u.root.querySelector(".legend");

        legendEl.classList.remove("inline");
        legendEl.classList.add("hidden");
        className && legendEl.classList.add(className);

        uPlot.assign(legendEl.style, {
            textAlign: "left",
            pointerEvents: "none",
            display: "none",
            position: "absolute",
            left: 0,
            top: 0,
            zIndex: 100,
            boxShadow: "2px 2px 10px rgba(0,0,0,0.5)",
            ...style
        });

        // hide series color markers
        const idents = legendEl.querySelectorAll(".ident");

        for (let i = 0; i < idents.length; i++)
            idents[i].style.display = "none";

        const overEl = u.root.querySelector(".over");
        overEl.style.overflow = "visible";

        // move legend into plot bounds
        overEl.appendChild(legendEl);

        // show/hide tooltip on enter/exit
        overEl.addEventListener("mouseenter", () => {legendEl.style.display = null;});
        overEl.addEventListener("mouseleave", () => {legendEl.style.display = "none";});

        // let tooltip exit plot
    //	overEl.style.overflow = "visible";
    }

    function update(u) {
        const { left, top } = u.cursor;
        legendEl.style.transform = "translate(" + left + "px, " + top + "px)";
    }

    return {
        hooks: {
            init: init,
            setCursor: update,
        }
    };
}

function dataVolumePlugin({ gap = 2, shadowColor = "#000011", redColor = "#e54245", greenColor = "#79ffa0", bodyMaxWidth = 20, shadowWidth = 2, bodyOutline = 1 } = {}) {

    function drawFlowAndVolume(u) {
        u.ctx.save();

        const offset = (shadowWidth % 2) / 2;

        u.ctx.translate(offset, offset);

        let [iMin, iMax] = u.series[0].idxs;

        let vol0AsY = u.valToPos(0, "vol", true);

        for (let i = iMin; i <= iMax; i++) {
            let xVal         = u.scales.x.distr == 2 ? i : u.data[0][i];
            let min         = u.data[1][i];
            let avg         = u.data[2][i];
            let max          = u.data[3][i];
            let vol          = u.data[4][i];

            let timeAsX      = u.valToPos(xVal,  "x", true);
            let minAsY       = u.valToPos(min,   "y", true);
            let avgAsY      = u.valToPos(avg,  "y", true);
            let maxAsY      = u.valToPos(max,  "y", true);
            let volAsY       = u.valToPos(vol, "vol", true);


            // shadow rect
            let shadowHeight = Math.max(maxAsY, minAsY) - Math.min(maxAsY, minAsY);
            let shadowX      = timeAsX - (shadowWidth / 2);
            let shadowY      = Math.min(maxAsY, minAsY);

            u.ctx.fillStyle = shadowColor;
            u.ctx.fillRect(
                Math.round(shadowX),
                Math.round(shadowY),
                Math.round(shadowWidth),
                Math.round(shadowHeight),
            );

            // body rect
            let columnWidth  = u.bbox.width / (iMax - iMin);
            let bodyWidth    = Math.min(bodyMaxWidth, columnWidth - gap);
            let bodyHeight   = maxAsY - minAsY;
            let bodyX        = timeAsX - (bodyWidth / 2);
            let bodyY        = avgAsY - (avgAsY - Math.min(minAsY, maxAsY))/2 ;
            let bodyColor    = redColor;

            u.ctx.fillStyle = shadowColor;
            u.ctx.fillRect(
                Math.round(bodyX),
                Math.round(bodyY),
                Math.round(bodyWidth),
                Math.round(bodyHeight),
            );

            // volume rect
            u.ctx.fillStyle = greenColor;

            u.ctx.fillRect(
                Math.round(bodyX),
                Math.round(volAsY),
                Math.round(bodyWidth),
                Math.round(vol0AsY - volAsY),
            );


            // throughput block rect
            u.ctx.fillStyle = redColor;
            u.ctx.fillRect(
                Math.round(bodyX + bodyOutline),
                Math.round(bodyY + bodyOutline),
                Math.round(bodyWidth - bodyOutline * 2),
                Math.round(bodyHeight - bodyOutline * 2),
            );
        }

        u.ctx.translate(-offset, -offset);

        u.ctx.restore();
    }

    return {
        opts: (u, opts) => {
            uPlot.assign(opts, {
                cursor: {
                    points: {
                        show: false,
                    }
                }
            });

            opts.series.forEach(series => {
                series.paths = () => null;
                series.points = {show: false};
            });
        },
        hooks: {
            draw: drawFlowAndVolume,
        }
    };
}

class VolumeLatencyHisto {
    setup(data, element, clickHandler) {
        let volume = data[4];
        const fmtDate = uPlot.fmtDate("{YYYY}-{MM}-{DD} {h}:{mm}:{ss}");
        const tzDate = ts => uPlot.tzDate(new Date(ts * 1e3), "Etc/UTC");
        const opts = {
            width: 1800,
            height: 500,
            //title: "Dataflow Latency and Volume",
            tzDate,
            plugins: [
                columnHighlightPlugin(),
                legendAsTooltipPlugin(),
                dataVolumePlugin()
            ],
            scales: {
                x: {
                    distr: 2,
                },
                y: {
                    range: (u, minOld, maxOld)  => {
                        let min = Infinity;
                        let max = -Infinity;
                        let heatmapMax = 0;
                        u.data[2].forEach(yVal => {
                              min = Math.min(min, yVal);
                              max = Math.max(max, yVal);
                        })
                        return [min * 0.95, max * 1.02];
                    },
                },
                vol: {
                     range: (u, minOld, maxOld)  => {
                            let min = Infinity;
                            let max = -Infinity;
                            let heatmapMax = 0;
                            u.data[4].forEach(yVal => {
                                  min = Math.min(min, yVal);
                                  max = Math.max(max, yVal);
                            })
                            return [min, max * 5];
                        },
                },
            },
            series: [
                {
                    label: "Date",
                    value: (u, ts) => fmtDate(tzDate(ts)),
                },
                {
                    label: "Min",
                    value: (u, v) => fmtDecimals(v, 2),
                },
                {
                    label: "Avg",
                    value: (u, v) => fmtDecimals(v, 2),
                },
                {
                    label: "Max",
                    value: (u, v) => fmtDecimals(v, 2),
                },
                {
                    label: "Volume",
                    scale: 'vol',
                    value: (u, v) => fmtDecimals(v, 0),
                },
            ],
            axes: [
                {},
                {
                    values: (u, vals) => vals.map(v => shortFmtDecimals(v, 0)+ " ms"),
                    size: 60,
                },
                {
                    side: 1,
                    scale: 'vol',
                    grid: {show: false},
                    values: (u, vals) => vals.map(v => shortFmtDecimals(v, 0))
                }
            ]
        };

        let uplot = new uPlot(opts, data, element);
        uplot.root.querySelector(".over").addEventListener('click', function(e1, e2) {
                const {left, top, idx} = uplot.cursor;
                let xValTime = uplot.data[0][idx];
                clickHandler(xValTime)
            })
        this.uPlot = uplot;
    }
    update(data) {
        this.uPlot.setData(data);
    }
}

