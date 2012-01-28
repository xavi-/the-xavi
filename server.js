var http = require("http");
var url = require("url");
var fs = require("fs");
var bind = require("bind");
var markdown = require("markdown").markdown;
var chn = require("./libraries/xavlib/channel");
var bee = require("beeline");

process.title = "node-xavi.co";

var DefaultBindHandler = (function() {
    var bindLink = (function() {
        var template = { "selected": "<span>(:name:)</span>",
                         "not-selected": "<a href='(:url:)'>(:name:)</a>" };
    
        return function(callback, val, context) {
            val = JSON.parse(val);
        
            if(context.id === val.id) { bind.to(template["selected"], val, callback); }
            else { bind.to(template["not-selected"], val, callback); }
        };
    })();
    
    function handler(context, req, res) {
        bind.toFile("./content/templates/default.html", context, function(data) {
            res.writeHead(200, { "Conent-Length": data.length,
                                 "Content-Type": "text/html" });
            res.end(data, "utf8");
        });
    }
    
    function bindMarkdown(callback, val) {
        callback(markdown.toHTML(val,"Maruku"), {}, true); 
    }

    return function(path, id, extContext) {
        if(Object.prototype.toString.call(id) !== "[object String]") { extContext = id; id = ""; }
        
        var context = { id: id,
                        link: bindLink,
                        markdown: bindMarkdown,
                        content: function(callback) { callback("(: file ~ " + path + " :)"); } };
        
        for(var i in extContext) { context[i] = extContext[i]; }
        
        return function(req, res) { handler(context, req, res); }; 
    };
})();

var router = bee.route({
    "`404`": DefaultBindHandler("./content/404.html"),
    
    "/robots.txt": bee.staticFile("./content/robots.txt", "text/plain"),
    "/client.js": bee.staticFile("./libraries/xavlib/channel/client.js", "application/x-javascript"),
    
    "r`^/pics/(.*)$`": bee.staticDir("./content/pics/", { ".gif": "image/gif", ".png": "image/png",
                                                          ".jpg": "image/jpeg", ".jpeg": "image/jpeg" }),
    
    "/ /index.html": DefaultBindHandler("./content/index.html", "home"),
    "/about-me": DefaultBindHandler("./content/about-me.html", "about-me"),
    "/resume": DefaultBindHandler("./content/resume.html", "resume"),
    "/key-remapper": DefaultBindHandler("./content/key-remapper.html", "key-map"),
    "/magnetic-poetry": DefaultBindHandler("./content/magnetic-poetry.html", "mag-poetry"),
    "/tv-schedule-downloader": DefaultBindHandler("./content/tv-schedule-downloader.html", "tv-download"),
    "/visual-sort": DefaultBindHandler("./content/visual-sort.html", "visual-sort"),
    
    "/interview": bee.staticFile("./content/interview.html", "text/html"),
    "/js-idiom /js-idioms": bee.staticFile("./content/js-idioms.html", "text/html"),
    
    "/articles /articles/ /articles/index.html": DefaultBindHandler("./content/articles/index.html", "articles"),
    "/articles/operation-is-not-supported-code-9":
        DefaultBindHandler("./content/articles/operation-is-not-supported-code-9.html"),
    "/articles/cool-chrome-eye-candy": DefaultBindHandler("./content/articles/cool-chrome-eye-candy.html"),
    "/articles/qunit-html-template": DefaultBindHandler("./content/articles/qunit-html-template.html"),
    "/articles/fun-with-tostring-in-javascript":
        DefaultBindHandler("./content/articles/fun-with-tostring-in-javascript.html"),
    "/articles/fun-with-valueof-in-javascript":
        DefaultBindHandler("./content/articles/fun-with-valueof-in-javascript.html"),
    "/articles/jquery-ui-css-themes-hosted-on-cdn":
        DefaultBindHandler("./content/articles/jquery-ui-css-themes-hosted-on-cdn.html"),
    "/articles/function-caller-chromes-leaky-abstraction":
        DefaultBindHandler("./content/articles/function-caller-chromes-leaky-abstraction.html"),
    "/articles/trouble-with-touch-events-jquery":
        DefaultBindHandler("./content/articles/trouble-with-touch-events-jquery.html"),
    "/articles/get-around-cross-domain-restrictions":
        DefaultBindHandler("./content/articles/get-around-cross-domain-restrictions.html"),
    "/articles/running-rails-app-from-github":
        DefaultBindHandler("./content/articles/running-rails-app-from-github.html"),
    
    "/drag-shapes /drag-shapes/ /drag-shapes/index.html":
        DefaultBindHandler("./content/drag-shapes/index.html", "drag-shapes"),
    "/drag-shapes/get-the-goal": DefaultBindHandler("./content/drag-shapes/get-the-goal.html"),
    "/drag-shapes/rotate-image": DefaultBindHandler("./content/drag-shapes/rotate-image.html"),
    "/drag-shapes/slanted-box": DefaultBindHandler("./content/drag-shapes/slanted-box.html"),
    "/drag-shapes/comet-drag":
        DefaultBindHandler("./content/drag-shapes/comet-drag.html", require("./apps/comet-drag").start(chn))
});

chn.init(router);

process.title = "node-xavi.co";
var server = http.createServer(router);

server.listen(8004);
console.log("Serving xavi.co on port 8004");