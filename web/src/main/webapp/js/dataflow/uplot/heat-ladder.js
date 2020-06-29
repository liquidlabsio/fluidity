function rawData(xCount, ySeriesCount, yCountMin, yCountMax, yMin, yMax) {
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

function generateRawData(){
    console.time("rawData");
    let raw = rawData();
    console.timeEnd("rawData");
//    return [
//        raw[0],
//        raw[1].map(vals => vals[0]),
//        raw[1].map(vals => vals[vals.length - 1]),
//        raw[1],
//    ];
    return raw;
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
        raw[1].map(vals => vals[0]),
        raw[1].map(vals => vals[vals.length - 1]),
        agg[1],
        agg[2],
    ];
}

    function heatmapPlugin2() {
        // let global min/max
        function fillStyle(count, minCount, maxCount) {
        //	console.log(val);
            return `hsla(${180 + count/maxCount * 180}, 80%, 50%, 1)`;
        }

        return {
            hooks: {
                draw: u => {
                    const { ctx, data } = u;
                    const bucketIncr = 2;

                    let yData = data[3];
                    let yQtys = data[4];
/*
                    let maxCount = -Infinity;
                    let minCount = Infinity;
                    yQtys.forEach(qtys => {
                        maxCount = Math.max(maxCount, Math.max.apply(null, qtys));
                        minCount = Math.min(minCount, Math.min.apply(null, qtys));
                    });
                    console.log(maxCount, minCount);
*/

                    // pre-calc rect height since we know the aggregation bucket
                    let yHgt = Math.floor(u.valToPos(bucketIncr, 'y', true) - u.valToPos(0, 'y', true));

                    yData.forEach((yVals, xi) => {
                        let xPos = Math.floor(u.valToPos(data[0][xi], 'x', true));

                        let maxCount = yQtys[xi].reduce((acc, val) => Math.max(val, acc), -Infinity);

                        yVals.forEach((yVal, yi) => {
                            let yPos =  Math.floor(u.valToPos(yVal, 'y', true));

                        //	ctx.fillStyle = fillStyle(yQtys[xi][yi], minCount, maxCount);
                            ctx.fillStyle = fillStyle(yQtys[xi][yi], 1, maxCount);
                            ctx.fillRect(
                                xPos - 4,
                                yPos,
                                10,
                                yHgt,
                            );
                        });
                    });
                }
            }
        };
    }

class HeatLadder {
    setup(data, element) {

        const opts = {
            width: 1800,
            height: 600,
            //title: "Latency Heatmap Aggregated 10ms (~20k)",
            plugins: [
                heatmapPlugin2(),
            ],
            cursor: {
                drag: {
                    y: true,
                },
                points: {
                    show: false
                }
            },
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
        };

        this.uPlot = new uPlot(opts, data, element);
    }
    update(data) {
        this.uPlot.setData(data);
    }
}