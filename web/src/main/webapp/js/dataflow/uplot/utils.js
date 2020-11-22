var inf = Infinity;

function fmtDecimals(val, dec) {
    return val.toFixed(dec).replace(/\d(?=(\d{3})+(?:\.|$))/g, "$&,");
}


function shortFmtDecimals(val, dec) {
    if (val > 999 && val < 900000) return val/1000 + "k"
    if (val >= 1000000) return fmtDecimals(val/1000000, dec) + "m"
    if (val >= 10 * 1000000) return fmtDecimals(val/10 * 1000000, dec) + "b"
    return fmtDecimals(val, dec);
}
function incrRound(num, incr) {
    return Math.round(num/incr)*incr;
}
function incrRoundUp(num, incr) {
    return Math.ceil(num/incr)*incr;
}
function incrRoundDn(num, incr) {
    return Math.floor(num/incr)*incr;
}
function getMinMax(data) {
    var _min = inf;
    var _max = -inf;

    for (var i = 0; i < data.length; i++) {
        if (data[i] != null) {
            _min = Math.min(_min, data[i]);
            _max = Math.max(_max, data[i]);
        }
    }
    return [_min, _max];
}
function offsetFactor(data, offset1, offset2) {
    return [data[0] * offset1, data[1] * offset2];
}

function randInt(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// ==================================================================