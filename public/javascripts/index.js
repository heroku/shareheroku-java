$(function() {

    $("#titleLink").bind('click', goToHome)

    $("#allAppsLink").bind('click', goToHome)

    $("#submitLink").bind('click', goToSubmit)

    $("#searchText").bind('keyup', goToSearch)

    $("#searchForm").bind('submit', goToSearch)

    $("#clearSearch").bind('click', goToHome)

    $('#deployDetails').on('hidden', function () {
        $(".modal-header").empty()
        $(".modal-body").empty()
    })

    var History = window.History

    History.Adapter.bind(window,'statechange', function() {
        var State = History.getState()
        updatePage(window.location)
    })

    // check the url
    updatePage(window.location)

})

function goToHome(event) {
    event.preventDefault()
    History.pushState({}, window.title, "/")
}

function goToSubmit(event) {
    event.preventDefault()
    History.pushState({}, window.title, "/submit")
}

function goToTag(event) {
    event.preventDefault()

    if (event.ctrlKey) {
        if (selectedTags.indexOf(event.data.tagId) >= 0) {
            selectedTags.splice(selectedTags.indexOf(event.data.tagId), 1)
        }
        else {
            selectedTags.push(event.data.tagId)
        }
    }
    else {
        selectedTags.splice(0, selectedTags.length, event.data.tagId)
    }

    // get the tag names

    if (selectedTags.length > 0) {
        History.pushState({tags: selectedTags}, "Tag: " + event.data.name, "/tag/" + selectedTags)
    }
    else {
        goToHome(event)
    }
}

function goToSearch(event) {
    if (event.type == "submit") {
        event.preventDefault()
    }

    var q = $("#searchText").val()
    if (q != "") {
        History.pushState({query: q}, "Search: " + q, "/search/" + q)
    }
    else {
        goToHome(event)
    }
}

function goToApp(event) {
    event.preventDefault()
    History.pushState({appId: event.data.appId}, "App: " + event.data.title, "/app/" + event.data.appId)
}

function updatePage(location) {

    $("#mainContent").empty()

    // parse url
    // /search/{query}
    // /tag/{selectedTags}
    // /app/{appId}

    var pathname = location.pathname

    var arr = pathname.split("/")

    appId = ""
    query = ""
    selectedTags = []
    showTags = true

    $("#allAppsLink").parent().removeClass("active")
    $("#submitLink").parent().removeClass("active")

    if (arr[1] == "search") {
        query = arr[2]
    }
    else if (arr[1] == "tag") {
        selectedTags = arr[2].split(",")
    }
    else if (arr[1] == "app") {
        appId = arr[2]
    }
    else if (arr[1] == "submit") {
        $("#submitLink").parent().addClass("active")
        showTags = false

        renderSubmitForm()
    }
    else {
        $("#allAppsLink").parent().addClass("active")
    }

    $("#searchText").val(query)
    if (query != "") {
        $("#clearSearch").show()
    }
    else {
        $("#clearSearch").hide()
    }

    fetchTags()

    fetchApps()
}

function fetchApps() {
    $("#mainContent").append('<div id="appTemplates" class="span9"></div>')

    $.get(location.pathname + location.search, function(data) {
        if ($.isArray(data)) {

            $("#appTemplates").empty()

            // tags or search

            if (data.length == 0) {
                $("#appTemplates").append("<div class='alert'><h4>Your query didn't match any apps</h4></div>")
            }

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

                t.append("<div><button id='appId-" + item.appId + "' class='btn'>Get Details</button></div>")

                s.append(t)

                // after it's on the dom
                $("#appId-" + item.appId).bind("click", item, goToApp)

            })
        }
        else if (data.appId !== undefined) {

            // individual app

            var t = $("<div class='thumbnail container-fluid'></div>")

            var row = $("<div class='app-details row-fluid'></div>")
            t.append(row)

            var l = $("<div class='span8'></div>")
            l.append("<h2>" + data.title + "</h2>")
            l.append("<p>" + data.description + "</p>")
            l.append("<h5>Demo: <a href='" + data.demoUrl + "' target='_blank'>" + data.demoUrl + "</a></h5>")
            l.append("<h5>Source: <a href='" + data.sourceUrl + "' target='_blank'>" + data.sourceUrl + "</a></h5>")
            l.append("<h5>Documentation: <a href='" + data.documentationUrl + "' target='_blank'>" + data.documentationUrl + "</a></h5>")

            row.append(l)

            var rc = $("<div class='span4'></div>")
            var r = $("<div class='app-actions'></div>")
            rc.append(r)

            r.append(getRating(data))
            r.append("<h3>Deploy on Heroku:</h3>")
            r.append("<input id='emailAddress' type='text' placeholder='Email Address'/>")
            r.append("<button id='deployAppButton' class='btn disabled'>Deploy on Heroku</button>")

            row.append(rc)

            var comments = $("<div class='comments'></div>")
            comments.append("<hr/>")
            comments.append("<h3>Comments</h3>")
            comments.append("<p>blah blah comments</p>")

            //t.append(comments)

            $("#appTemplates").append(t)

            $("#emailAddress").bind('keyup', data, deployApp)
            $("#deployAppButton").bind('click', data, deployApp)
        }
    }, "json")
}

function fetchTags() {
    // fetch the tags
    if (tags.length == 0) {
        $.get("/tags", renderTags, "json")
    }
    else {
        renderTags(tags)
    }
}

function renderTags(data) {
    tags = data

    // nasty
    if (typeof updateTags !== "undefined") {
        updateTags()
    }

    if (showTags) {

        $("#mainContent").prepend('<div class="span3"><div class="well sidebar-nav"><ul id="tags" class="nav nav-list"><li class="nav-header">Tags</li></ul></div></div>')

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

        $("#tags a").parent().removeClass("active")

        for (var i = 0; i < selectedTags.length; i++) {
            $("#tagId-"+selectedTags[i]).parent().addClass("active")
        }
    }
}

function renderSubmitForm() {

    $.get("/public/submitForm.html", function(data) {
        $("#mainContent").append(data)
    }, "text")

}

function getRating(appTemplate) {
    var rs = ""
    for (var i = 1; i <= 5; i++) {
        if (i <= appTemplate.rating) {
            rs += '<a href="#"><i class="icon-star"></i></a>'
        }
        else {
            rs += '<a href="#"><i class="icon-star-empty"></i></a>'
        }
    }

    return rs
}

function deployApp(event) {

    // check the email format
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

    var e = $('#emailAddress').val()

    if (re.test(e)) {
        $("#deployAppButton").removeClass("disabled")
    }
    else {
        $("#deployAppButton").addClass("disabled")
    }

    if (((event.type == "click") || ((event.type == "keyup") && (event.keyCode == 13))) && ($("#deployAppButton").hasClass("disabled") == false))  {

        $(".modal-header").append('<h3>Deploying App on Heroku</h3>')
        $(".modal-body").append('<div class="progress progress-info progress-striped active"><div class="bar" style="width: 100%;"></div></div>')
        $("#deployDetails").modal()

        var o = {}
        o.emailAddress = $("#emailAddress").val()
        o.appId = event.data.appId;

        $.post("/shareApp", o, function(data) {
            $(".progress").remove()
            $(".modal-header").empty()
            $(".modal-header").append('<a class="close" data-dismiss="modal">&times;</a>')
            $(".modal-header").append('<h3>App Deployed on Heroku!</h3>')
            $(".modal-body").append('<div class="alert alert-success">Web URL: <a href="' + data.web_url + '">' + data.web_url + '</a><br/>Git URL: ' + data.git_url + '</div>')

            // todo: instructions from event.data.instructionsUrl

        }, "json").error(function(error) {
           var resp = $.parseJSON(error.responseText)
           $(".progress").remove()
           $(".modal-body").append("<div class='alert alert-error'><strong>Error:</strong> " + resp.message + "</div>")
        })
    }
}

var appId = ""
var query = ""
var selectedTags = []
var tags = []
var showTags = true