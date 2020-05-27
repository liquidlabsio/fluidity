SERVICE_URL = 'http://localhost:8080'
DEFAULT_TENANT='tenant-data-1'

function checkUserLogin(){
    Fluidity.username = "unknown"
    if (window.location.pathname.endsWith("index.html")) {
        let auth = window.localStorage.getItem("fluidity-auth")
        if (auth == null || auth.length == 0) {
            alert("User is not logged in")
            window.location.href = "signin.html"
        } else {
            let timestamp = parseInt(auth.split(":")[0])
            Fluidity.username = auth.split(":")[2]
            let sessionAgeHours = (new Date().getTime() - timestamp)/(1000 * 60 * 12);
            if (sessionAgeHours > 12 /** 12 hours old **/) {
                alert("Session has expired")
                window.location.href = "signin.html"
            }
        }
    }
}

if (typeof Fluidity == 'undefined') {
    console.log("Fluidity created")



    // initialize bootstrap tooltips
    $('[data-toggle="tooltip"]').tooltip(
            {
                placement : 'bottom',
                delay: 500
            });


    Fluidity = {
        Explorer: {},
        Search: {},
        Refinery: {},

        startOfWord: function (txt, end) {
            var i = end -1;

            while (i > 0 && txt.charAt(i) !== ' ') {
                i--;
            }

            return i+1;
        },
        endOfWord: function (txt, start) {
            var i = start;
            while (i < txt.length && txt.charAt(i) !== ' ') {
                i++;
            }

            return i;
        },
        getQueryParams: function () {
            var qs = window.location.search;
            qs = qs.split("+").join(" ");

            var params = {}, tokens,
                re = /[?&]?([^=]+)=([^&]*)/g;

            while (tokens = re.exec(qs)) {
                params[decodeURIComponent(tokens[1])]
                    = decodeURIComponent(tokens[2]);
            }

            return params;
        },
        setUrlParameter: function(paramName, paramValue)  {
            var url = window.location.href;
            if (url.indexOf(paramName + "=") >= 0)
            {
                var prefix = url.substring(0, url.indexOf(paramName));
                var suffix = url.substring(url.indexOf(paramName));
                suffix = suffix.substring(suffix.indexOf("=") + 1);
                suffix = (suffix.indexOf("&") >= 0) ? suffix.substring(suffix.indexOf("&")) : "";
                url = prefix + paramName + "=" + paramValue + suffix;
            }
            else
            {
                if (url.indexOf("?") < 0)
                    url += "?" + paramName + "=" + paramValue;
                else
                    url += "&" + paramName + "=" + paramValue;
            }
            history.pushState(null, "Fluidity", url);
        },

        containsAny: function (string, valuesArray) {
            string = string.toLowerCase();
            var result = false;
            valuesArray.forEach(function(vv) {
                vv = vv.trim().toLowerCase()
                if (!result && string.indexOf(vv) != -1) result = true;
            })
            return result;
        },
        replaceRegion: function (value, into, sIdx, eIdx) {
            return into.slice(0, sIdx) + value + into.slice(eIdx);
        },
        addCommas: function (nStr)  {
            nStr += '';
            x = nStr.split('.');
            x1 = x[0];
            x2 = x.length > 1 ? '.' + x[1] : '';
            var rgx = /(\d+)(\d{3})/;
            while (rgx.test(x1)) {
                x1 = x1.replace(rgx, '$1' + ',' + '$2');
            }
            return x1 + x2;
        },
        getLocale: function() {
            var current = localStorage.getItem("fluidity.lang");
            if (current != null) return current;
            return "en"
        },
        formatNumber: function(num) {
            return num.toString().replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,')
        },
        Components: {},
        Util: {
            UUID: function UUID() {
                var uuid = (function () {
                    var i,
                        c = "89ab",
                        u = [];
                    for (i = 0; i < 36; i += 1) {
                        u[i] = (Math.random() * 16 | 0).toString(16);
                    }
                    u[8] = u[13] = u[18] = u[23] = "-";
                    u[14] = "4";
                    u[19] = c.charAt(Math.random() * 4 | 0);
                    return u.join("");
                })();
                return {
                    toString: function () {
                        return uuid;
                    },
                    valueOf: function () {
                        return uuid;
                    }
                };
            }
        },
        Notify: {},
        WebSockets: {},
        ClickHandler: function (delegate) {
            return function (event) {
                delegate(event)
                return false
            }
        },
        DecodeJson: function(delegate) {
            return function(data) {
                delegate($.parseJSON(data));
            }
        }
    };


    checkUserLogin()

    // create the jquery topics for this workspace..
    var topics = {};

    jQuery.Topic = function (id) {
        var callbacks,
            method,
            topic = id && topics[ id ];

        if (!topic) {
            callbacks = jQuery.Callbacks();
            topic = {
                publish: callbacks.fire,
                subscribe: callbacks.add,
                unsubscribe: callbacks.remove
            };
            if (id) {
                topics[ id ] = topic;
            }
        }
        return topic;
    }


    navigator.sayswho = (function () {
        var N = navigator.appName, ua = navigator.userAgent, tem;
        var M = ua.match(/(phantom|opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
        if (M && (tem = ua.match(/version\/([\.\d]+)/i)) != null) M[2] = tem[1];
        M = M ? [M[1], M[2]] : [N, navigator.appVersion, '-?'];
        if (ua.indexOf("PhantomJS") != -1) return "phantomjs";//, "1.9.1"];
        return M;
    })();

    console.log("USERAGENT:" + navigator.userAgent)
    console.log("PLATFORM:" + navigator.sayswho)

    // load the user agent
    var b = document.documentElement;
    b.setAttribute('data-useragent',  navigator.sayswho);

    if (navigator.sayswho[0].indexOf("Chrome") != -1 && parseInt(navigator.sayswho[1]) < 25) {
        window.alert("Upgrade Chrome to Version 25+, You have version: "+ navigator.sayswho)
    }
    if (navigator.sayswho[0].indexOf("MSIE") != -1 && parseInt(navigator.sayswho[1]) < 10) {
        window.alert("Upgrade IE to Version 10+, You have version: "+ navigator.sayswho)
    }
    if (navigator.sayswho[0].indexOf("Fire") != -1 && parseInt(navigator.sayswho[1]) < 24) {
        window.alert("Upgrade FireFox to Version 24+, You have version: "+ navigator.sayswho)
    }
    if (navigator.sayswho[0].indexOf("Safari") != -1 && parseInt(navigator.sayswho[1]) < 6) {
        window.alert("Upgrade Safari to Version 6+, You have version: "+ navigator.sayswho)
    }
}