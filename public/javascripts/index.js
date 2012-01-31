$(function() {

    $("#titleLink").bind('click', goToHome)
    $("#allAppsLink").bind('click', goToHome)

    $("#searchText").bind('enter', goToSearch)
    $("#searchButton").bind('click', goToSearch)

    var History = window.History

    History.Adapter.bind(window,'statechange', function() {
        var State = History.getState()
        updatePage(window.location)
    })

    // check the url
    updatePage(window.location)

    // fetch the tags
    $.get("/tags", function(data) {
        tags = data

        $.each(data, function(index, item) {
            var a = $("<a>" + item.name + "</a>")
            a.attr("id", "tagId-" + item.tagId)
            a.attr("href", "/tag/" + item.tagId)
            if (History.enabled) {
                a.bind('click', item, goToTag)
            }
            var li = $("<li/>")
            li.append(a)
            var li = $("#tags").append(li)
        })
        updateActiveTag()
    }, "json")

})

function goToHome(event) {
    event.preventDefault()
    History.pushState({}, window.title, "/")
}

function goToTag(event) {
    event.preventDefault()
    History.pushState({tagId: event.data.tagId}, "Tag: " + event.data.name, "/tag/" + event.data.tagId)
}

function goToSearch(event) {
    event.preventDefault()
    var q = $("#searchText").val()
    History.pushState({query: q}, "Search: " + q, "/search/" + q)
}

function goToApp(event) {
    event.preventDefault()
    History.pushState({appId: event.data.appId}, "App: " + event.data.title, "/app/" + event.data.appId)
}

function updatePage(location) {

    // parse url
    // /search/{query}
    // /tag/{tagId}
    // /app/{appId}

    var pathname = location.pathname

    var arr = pathname.split("/")

    appId = ""
    query = ""
    tagId = ""

    if (arr.length == 3) {

        if (arr[1] == "search") {
            query = arr[2]
        }
        else if (arr[1] == "tag") {
            tagId = arr[2]
        }
        else if (arr[1] == "app") {
            appId = arr[2]
        }

        $("#allAppsLink").parent().removeClass("active")
    }
    else {
        $("#allAppsLink").parent().addClass("active")
    }

    updateActiveTag()

    fetchApps()
}

function fetchApps() {
    $("#appTemplates").empty()

    $.get(location.pathname, function(data) {
        if ($.isArray(data)) {

            // tags or search

            $.each(data, function(index, item) {
                if (index % 3 == 0) {
                    $("#appTemplates").append("<div class='row-fluid app-row'></div>")
                }

                var d = $("#appTemplates").children().last()

                var s = $("<div class='span4'></div>")
                d.append(s)

                var t = $("<div class='app-item thumbnail'></div>")
                t.append("<h4>" + item.title + "</h4>")

                var ts = ""
                $.each(item.tags, function(tagIndex, tagItem) {
                    ts += tagItem.name
                    if (tagIndex != item.tags.length -1) {
                        ts += ", "
                    }
                })

                var tags = $("<h6>Tags: " + ts + "</h6>")

                t.append(tags)

                var rating = $("<h6>Rating: " + getRating(item) + "</h6>")
                t.append(rating)

                t.append("<div><button id='appId-" + item.appId + "' class='btn primary'>Get Details</button></div>")

                s.append(t)

                // after it's on the dom
                $("#appId-" + item.appId).bind("click", item, goToApp)

            })
        }
        else if (!(typeof data.appId === undefined)) {

            var t = $("<div class='thumbnail container-fluid'></div>")

            var row = $("<div class='row-fluid app-details'></div>")
            t.append(row)

            var l = $("<div class='span9'></div>")
            l.append("<h2>" + data.title + "</h2>")
            l.append("<p>" + data.description + "</p>")
            l.append("<h5>Demo: <a href='" + data.demoUrl + "' target='_blank'>" + data.demoUrl + "</a></h5>")
            l.append("<h5>Source: <a href='" + data.sourceUrl + "' target='_blank'>" + data.sourceUrl + "</a></h5>")
            l.append("<h5>Documentation: <a href='" + data.documentationUrl + "' target='_blank'>" + data.documentationUrl + "</a></h5>")
            l.append("<hr/>")
            l.append("<h3>Comments</h3>")
            l.append("<p>blah blah comments</p>")
            row.append(l)

            var rc = $("<div class='span3'></div>")
            var r = $("<div class='app-actions'></div>")
            rc.append(r)

            r.append(getRating(data))
            r.append("<h3>Deploy on Heroku:</h3>")
            r.append("<input type='text' placeholder='Email Address'/>")
            r.append("<button class='btn'>Copy and Deploy</button>")

            row.append(rc)

            $("#appTemplates").append(t)



                /*
            <div id="app" class="span9 thumbnail">
                <h3 id="appHeader"></h3>
                <a class="btn primary" href="#">Deploy on Heroku</a>
            </div>
            */
        }
    }, "json")
}

function updateActiveTag() {
    $("#tags a").parent().removeClass("active")

    if (tagId != "") {
        $("#tagId-"+tagId).parent().addClass("active")
    }
}

function getRating(appTemplate) {
    var rs = ""
    for (var i = 1; i <= 5; i++) {
        if (i <= appTemplate.rating) {
            rs += '<a href="#"><i class="icon star"></i></a>'
        }
        else {
            rs += '<a href="#"><i class="icon star-empty"></i></a>'
        }
    }

    return rs
}

var appId = ""
var query = ""
var tagId = ""
var tags = []