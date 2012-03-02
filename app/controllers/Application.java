package controllers;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.google.gson.Gson;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import models.AppTemplate;
import models.Tag;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import play.data.validation.Valid;
import play.db.jpa.GenericModel;
import play.libs.Mail;
import play.mvc.*;
import play.data.validation.Error;

import java.util.*;

//@With(Compress.class)
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

    // todo: revalidating the entire object every time one field changes is silly
    public static void submitApp(@Valid AppTemplate appTemplate, boolean validateOnly) throws EmailException {

        if(validation.hasErrors()) {
            if (request.format.equals("json")) {
                HashMap<String, List<String>> errors = new HashMap<String, List<String>>();
                
                for (Error error : validation.errors()) {
                    if (errors.get(error.getKey()) == null) {
                        errors.put(error.getKey(), new ArrayList<String>());
                    }
                    errors.get(error.getKey()).add(error.toString());
                }

                String errorsJson = new Gson().toJson(errors);
                error(errorsJson);
            }
            else {
                error(validation.errors().toString());
            }
        }

        if (!validateOnly) {
            appTemplate.save();

            if (System.getenv("HEROKU_USERNAME") != null) {
                MultiPartEmail email = new MultiPartEmail();

                email.setFrom(appTemplate.submitterEmail);
                email.addTo(System.getenv("HEROKU_USERNAME"));
                email.setSubject("New Template Submission");
                email.setMsg(appTemplate.toFullString());

                Mail.send(email);
            }

        }

        ok();
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
            
            GenericModel.JPAQuery jpaQuery = AppTemplate.find(BASE_APPTEMPLATE_SELECT + " JOIN a.tags AS tag WHERE UPPER(tag.tagId) IN :tags GROUP BY a.id, a.appId, a.title, a.description, a.demoUrl, a.sourceUrl, a.herokuAppName, a.documentationUrl, a.instructionsUrl, a.suggestedTags, a.rating, a.status, a.lastUpdated, a.submitterEmail HAVING COUNT(a.id) = :size " + getOrderBy());
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

        if (!validation.email(emailAddress).ok) {
            error("Invalid Email Address");
        }
        
        if (appTemplate == null) {
            error("Invalid Application");
        }

        if ((System.getenv("HEROKU_USERNAME") == null) || (System.getenv("HEROKU_API_KEY") == null)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // mock app
            HashMap<String, String> mockApp = new HashMap<String, String>();
            mockApp.put("name", "fake-app-1234");
            mockApp.put("web_url", "http://fake-app-1234.herokuapp.com");
            mockApp.put("git_url", "git@heroku.com:fake-app-1234.git");
            renderJSON(mockApp);
        }

        // create an app on heroku
        JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, JGoogleAnalyticsTracker.GoogleAnalyticsVersion.V_4_7_2);
        tracker.trackEvent("app", "shareApp", appTemplate.appId);

        HerokuAPI herokuAPI = new HerokuAPI(System.getenv("HEROKU_API_KEY"));
        
        App app = herokuAPI.getConnection().execute(new AppTemplateCreate(appTemplate.herokuAppName));

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
