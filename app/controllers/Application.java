package controllers;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.connection.HttpClientConnection;
import com.heroku.api.http.HttpUtil;
import com.heroku.api.request.RequestConfig;
import com.heroku.api.request.app.AppCreate;
import com.heroku.api.request.login.BasicAuthLogin;
import helpers.EmailHelper;
import models.AppTemplate;
import models.Tag;
import play.data.validation.Valid;
import play.db.jpa.GenericModel;
import play.mvc.*;
import play.data.validation.Error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Application extends Controller {

    private static final AnalyticsConfigData config = new AnalyticsConfigData("UA-26859570-1");
    private static final String DEFAULT_SORT = "a.rating";
    private static final String BASE_APPTEMPLATE_SELECT = "SELECT a FROM AppTemplate a";

    private static String getOrderBy() {

        String sort = request.params.get("sort");

        if (sort == null) {
            sort = DEFAULT_SORT;
        } else if (sort.equals("rating")) {
            sort = "a.rating DESC";
        } else if (sort.equals("title")) {
            sort = "a.title ASC";
        } else if (sort.equals("lastUpdated")) {
            sort = "a.lastUpdated ASC";
        }

        return "ORDER BY " + sort;
    }
    
    private static List<AppTemplate> fetchAppTemplates(GenericModel.JPAQuery jpaQuery) {
        String limit = request.params.get("limit");

        if (limit != null) {
            return jpaQuery.fetch(Integer.parseInt(limit));
        }
        else {
            return jpaQuery.fetch();
        }
    }

    public static void index() {
        if (request.format.equals("json")) {
            GenericModel.JPAQuery jpaQuery = AppTemplate.find(BASE_APPTEMPLATE_SELECT + " WHERE a.status = ? " + getOrderBy(), AppTemplate.Status.PUBLISHED);
            renderJSON(fetchAppTemplates(jpaQuery));
        }
        else {
            render();
        }
    }

    public static void submit() {
        if (request.format.equals("json")) {
            renderJSON("{}");
        }
        else {
            renderTemplate("Application/index.html");
        }
    }

    public static void submitApp(@Valid AppTemplate appTemplate) {
        if (request.format.equals("json")) {
            if(validation.hasErrors()) {
                throw new RuntimeException(validation.errorsMap().toString());
            }

            appTemplate.save();
        }
        else {
            if(validation.hasErrors()) {

            }

            appTemplate.save();
        }
    }
    
    public static void search(String query) {
        if (query.length() == 0) {
            index();
        }

        if (request.format.equals("json")) {
            GenericModel.JPAQuery jpaQuery = AppTemplate.find(BASE_APPTEMPLATE_SELECT + " WHERE (UPPER(a.title) like UPPER(?) or UPPER(a.description) like UPPER(?)) and status = ? " + getOrderBy(), "%" + query + "%", "%" + query + "%", AppTemplate.Status.PUBLISHED);
            renderJSON(fetchAppTemplates(jpaQuery));
        }
        else {
            renderTemplate("Application/index.html");
        }

    }

    public static void tag(String tagIds) {
        if (tagIds == null) {
            index();
        }

        if (request.format.equals("json")) {
            String[] tags = tagIds.split(",");

            for (int i = 0; i < tags.length; i++) {
                tags[i] = tags[i].toUpperCase();
            }
            
            GenericModel.JPAQuery jpaQuery = AppTemplate.find(BASE_APPTEMPLATE_SELECT + " JOIN a.tags AS tag WHERE UPPER(tag.tagId) IN :tags GROUP BY a.id HAVING COUNT(a.id) = :size " + getOrderBy());
            jpaQuery.bind("tags", tags);
            jpaQuery.bind("size", tags.length);

            renderJSON(fetchAppTemplates(jpaQuery));
        }
        else {
            renderTemplate("Application/index.html");
        }

    }

    public static void app(String appId) {
        if (request.format.equals("json")) {
            AppTemplate appTemplate = AppTemplate.find("byAppId", appId).first();
            renderJSON(appTemplate);
        }
        else {
            renderTemplate("Application/index.html");
        }

    }

    public static void tags() {
        if (request.format.equals("json")) {
            List<Tag> tags = Tag.find("SELECT tag From Tag tag ORDER BY tagId").fetch();
            renderJSON(tags);
        }
        else {
            error("Only requests that accept json are supported");
        }

    }

    public static void shareApp(String emailAddress, String appId) {

        AppTemplate appTemplate = AppTemplate.find("byAppId", appId).first();

        validation.email(emailAddress);

        if(validation.hasErrors()) {
            error(validation.errorsMap().toString());
        }
        else {
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, JGoogleAnalyticsTracker.GoogleAnalyticsVersion.V_4_7_2);
            tracker.trackEvent("app", "shareApp", appTemplate.appId);

            HttpClientConnection herokuConnection = new HttpClientConnection(new BasicAuthLogin(System.getenv("HEROKU_USERNAME"), System.getenv("HEROKU_PASSWORD")));
            HerokuAPI herokuAPI = new HerokuAPI(herokuConnection);

            // create an app on heroku (using heroku credentials specified in ${HEROKU_USERNAME} / ${HEROKU_PASSWORD}
            App app = herokuConnection.execute(new AppTemplateCreate(appTemplate.herokuAppName));

            if (!app.getCreateStatus().equals("complete")) {
                error("Could not create the Heroku app");
            }

            // share the app with the provided email
            herokuAPI.addCollaborator(app.getName(), emailAddress);

            // transfer the app to the provided email
            herokuAPI.transferApp(app.getName(), emailAddress);

            // remove ${HEROKU_USERNAME} as collaborator
            herokuAPI.removeCollaborator(app.getName(), System.getenv("HEROKU_USERNAME"));

            renderJSON(app);
        }
    }

}
