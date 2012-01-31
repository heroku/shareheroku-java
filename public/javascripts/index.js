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
    event.preventDefault();
    History.pushState({}, window.title, "/")
}

function goToTag(event) {
    event.preventDefault();
    History.pushState({tagId: event.data.tagId}, "Tag: " + event.data.name, "/tag/" + event.data.tagId)
}

function goToSearch(event) {
    event.preventDefault();
    var q = $("#searchText").val()
    History.pushState({query: q}, "Search: " + q, "/search/" + q)
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
    $("#app").empty()

    $.get(location.pathname, function(data) {
        if ($.isArray(data)) {
            // tags or search
            $("#app").hide()
            $("#appTemplates").show()

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

                t.append("<div><button class='btn primary'>Get Details</button></div>")

                s.append(t)


            })
        }
        else if (!(typeof data.appId === undefined)) {
            $("#app").append("<p>" + data.title + "</p>")
        }
    }, "json")
}

function updateActiveTag() {
    $("#tags a").parent().removeClass("active")

    if (tagId != "") {
        $("#tagId-"+tagId).parent().addClass("active")
    }
}

var appId = ""
var query = ""
var tagId = ""
var tags = []