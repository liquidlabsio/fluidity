    var ddd;
    var nnn;
    var uuu;

const nnnnData = [
    // dates
    [1546300800,1546387200,1546473600,1546560000,1546819200,1546905600,1546992000,1547078400,1547164800,1547424000,1547510400,1547596800],

    //  latency ladder
    [
       [],// [80,  50,  43],
        [10,  12,  14,  18,  30,  50],
        [70,  90,  13,  10,  12,  13],
        [170, 190, 113, 110, 112, 113],
        [270, 290, 213, 110, 112, 113],
        [80,  50,  43],
        [10,  12,  14,  18,  30,  50],
        [70,  90,  13,  10,  12,  13],
        [170, 190, 113, 110, 112, 113],
        [270, 290, 213, 110, 112, 113],
        [220, 222, 224, 228, 230, 240],
        [270, 290, 213, 210, 212, 213],

    ],

    // count ladder to match latency
    [
    [],
       // [1,  3,   5],
        [7,  9,   5,   6, 7,  8],
        [17, 119, 115, 116, 117, 118],
        [27, 29, 25, 226, 227, 228],
        [17, 19, 15, 216, 227, 288],
        [12,  32,   52],
        [217,  219,   252,   216, 217,  218],
        [17, 19, 15, 16, 17, 18],
        [27, 29, 25, 26, 27, 28],
        [17, 19, 15, 16, 27, 88],
        [17, 19, 15, 16, 17, 18],
        [27, 29, 25, 26, 27, 28],
    ],

    // colour range
    [10, 256]

];



function generateRawData(xCount, ySeriesCount, yCountMin, yCountMax, yMin, yMax) {
        xCount = xCount || 100;
        ySeriesCount = ySeriesCount || 1;

        // 50-300 samples per x
        yCountMin = yCountMin || 200;
        yCountMax = yCountMax || 500;

        // y values in 0 - 1000 range
        yMin = yMin || 5;
        yMax = yMax || 1000;

        let data = [
            [],
            ...Array(ySeriesCount).fill(null).map(_ => []),
        ];

        let now = Math.round(new Date() / 1e3);

        let finalCount = 0;

        for (let xi = 0; xi < xCount; xi++) {
            data[0][xi] = now++;

            for (let si = 1; si <= ySeriesCount; si++) {
                let yCount = randInt(yCountMin, yCountMax);

                let vals = data[si][xi] = [];

                while (yCount-- > 0) {
                //	vals.push(Math.round(randn_bm(yMin, yMax, 3)));
                    vals.push(Math.max(randomSkewNormal(Math.random, 30, 30, 3), yMin));
                    finalCount++;
                }

                vals.sort((a, b) => a - b);
            }
        }

        console.log(finalCount);

        return data;
    }


function aggData(data, incr) {
        let data2 = [
            data[0],
            [],
            [],
        ];

        data[1].forEach((vals, xi) => {
            let _aggs = [];
            let _counts = [];

            let _curVal = incrRoundUp(vals[0], incr);
            let _curCount = 0;

            vals.forEach(v => {
                v = incrRoundUp(v, incr);

                if (v == _curVal)
                    _curCount++;
                else {
                    _aggs.push(_curVal);
                    _counts.push(_curCount);

                    _curVal = v;
                    _curCount = 1;
                }
            });

            _aggs.push(_curVal);
            _counts.push(_curCount);

            data2[1][xi] = _aggs;
            data2[2][xi] = _counts;
        });

        return data2;
    }


// bucketIncr = 2
function generateAggData(raw, bucketIncr) {
	console.time("aggData");
    let agg = aggData(raw, bucketIncr);
	console.timeEnd("aggData");
//		console.log(agg);

    return [
        agg[0],
        agg[1],
        agg[2],
    ];
}
    function heatmapPlugin2(bucketIncr) {
        // let global min/max
        function fillStyle(count, minCount, maxCount) {
        //	console.log(val);
            return `hsla(${180 + count/maxCount * 180}, 80%, 50%, 1)`;
        }

        return {
            hooks: {
                draw: u => {
                    const { ctx, data } = u;

                    let yData = data[1];
                    let yQtys = data[2];
                    let [iMin, iMax] = u.series[0].idxs;

                    let maxCount = -Infinity;
                    let minCount = Infinity;
                    yQtys.forEach(qtys => {
                        maxCount = Math.max(maxCount, Math.max.apply(null, qtys));
                        minCount = Math.min(minCount, Math.min.apply(null, qtys));
                    });
//                    console.log(maxCount, minCount);


                    // pre-calc rect height since we know the aggregation bucket
                    let yHgt = Math.floor(u.valToPos(bucketIncr, 'y', true) - u.valToPos(0, 'y', true));

                    // prevent super skinny rendering
                    if (yHgt >= -2) {
                        yHgt = -5;
                    }
                    let columnWidth  = u.bbox.width / (iMax - iMin);

                    yData.forEach((yVals, xi) => {

                        let xPos = Math.floor(u.valToPos(data[0][xi], 'x', true));

                        yVals.forEach((yVal, yi) => {
                            let yPos =  Math.floor(u.valToPos(yVal, 'y', true));

                        	ctx.fillStyle = fillStyle(yQtys[xi][yi], minCount, maxCount);
                            //ctx.fillStyle = fillStyle(yQtys[xi][yi], 1, maxCount);
                            ctx.fillRect(
                                xPos - (columnWidth/2) - 5,
                                yPos,
                                columnWidth-4,
                                yHgt,
                            );
                        });
                    });
                }
            }
        };
    }

class HeatLadder {
    setup(data, element, clickHandler, bucketIncr) {

        const opts = {
            width: 1800,
            height: 600,
            //title: "Latency Heatmap Aggregated 10ms (~20k)",
            plugins: [
                heatmapPlugin2(bucketIncr),
            ],
            cursor: {
                drag: {
                    y: true,
                },
                points: {
                    show: false
                }
            },
            axes: [
                {},
                {
                    values: (u, vals) => vals.map(v => shortFmtDecimals(v, 0)+ " ms"),
                    size: 60,
            }],
            series: [
                {},
                {
                    paths: () => null,
                    points: {show: false},
                },
                {
                    paths: () => null,
                    points: {show: false},
                },
            ],
            scales: {
                y: {
                    auto: false,
                    range: (u, minOld, maxOld)  => {
                            let min = Infinity;
                            let max = -Infinity;
                            let heatmapMax = 0;
                            u.data[1].forEach(yValueArray => {
                                yValueArray.forEach(yVal => {
                                  min = Math.min(min, yVal);
                                  max = Math.max(max, yVal);
                                })
                            })
                            return [min, max];
                        },
        }
            }
        }
        let u = new uPlot(opts, data, element);

        u.root.querySelector(".over").addEventListener('click', function(e1, e2) {

            const {left, top, idx} = u.cursor;
            let xVal = u.data[0][idx];
            let yVal = u.data[1][idx];
            (u.valToPos(xVal, 'x'))
            let liveXValue = Math.round(u.posToVal(top, "x").toFixed(0));
            let liveYValue = Math.round(u.posToVal(top, "y"));
            console.log("yay:" + u.cursor.idx)
            clickHandler(u.cursor.idx, liveXValue, liveYValue);
        });
        this.uPlot = u;
    }
    update(data) {
        this.uPlot.setData(data);
    }
    click(time) {
        console.log("Clicked:" + time)
    }
}