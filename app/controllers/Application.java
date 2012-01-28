package controllers;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.google.gson.Gson;
import com.heroku.api.App;
import helpers.EmailHelper;
import helpers.HerokuAppSharingHelper;
import models.AppTemplate;
import models.Tag;
import play.libs.F;
import play.mvc.*;
import play.data.validation.Error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Application extends Controller {

    private static final AnalyticsConfigData config = new AnalyticsConfigData("UA-26859570-1");

    public static void index() {
        if (request.format.equals("json")) {
            List<AppTemplate> appTemplates = AppTemplate.findAll();
            renderJSON(appTemplates);
        }
        else {
            render();
        }
    }

    public static void shareApp(String emailAddress, String gitUrl) {
        validation.email(emailAddress);
        validation.minSize(gitUrl, 1); //todo: convert to matcher

        if(validation.hasErrors()) {
            Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
            errors.put("error", new HashMap<String, String>());
            for (Error error : validation.errors()) {
                errors.get("error").put(error.getKey(), error.message());
            }
            renderJSON(errors);
        }
        else {
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, JGoogleAnalyticsTracker.GoogleAnalyticsVersion.V_4_7_2);
            tracker.trackEvent("app", "shareApp", gitUrl);

            Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
            errors.put("error", new HashMap<String, String>());

            try {
                HerokuAppSharingHelper job = new HerokuAppSharingHelper(emailAddress, gitUrl);
                F.Promise<App> promiseAppMetadata = job.now();

                String encoding = Http.Response.current().encoding;
                response.setContentTypeIfNotSet("application/json; charset=" + encoding);

                // force this connection to stay open
                while (!promiseAppMetadata.isDone()) {
                    Thread.sleep(1000);
                    response.writeChunk("");
                }

                App app = await(promiseAppMetadata);

                if (job.exception != null) {
                    throw job.exception;
                }
                
                Map<String, App> result = new HashMap<String, App>();
                result.put("result", app);

                response.writeChunk(new Gson().toJson(result));
                return;
            }
            catch (Throwable e) {

                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    EmailHelper.sendEmailViaMailGun(System.getenv("HEROKU_USERNAME"), System.getenv("HEROKU_USERNAME"), "App Error: " + request.host, e.getMessage() + "\r\n" + sw.toString());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                errors.get("error").put("shareApp", e.getMessage());
            }

            if (errors.get("error") != null) {
                response.writeChunk(new Gson().toJson(errors));
            }
            
        }
    }
    
    public static void search(String query) {
        if (request.format.equals("json")) {
            List<AppTemplate> appTemplates = AppTemplate.find("(UPPER(title) like UPPER(?) or UPPER(description) like UPPER(?)) and status = ?", "%" + query + "%", "%" + query + "%", AppTemplate.Status.PUBLISHED).fetch();
            renderJSON(appTemplates);
        }
        else {
            renderTemplate("Application/index.html");
        }

    }

    public static void tag(String tagId) {
        if (request.format.equals("json")) {
            List<AppTemplate> appTemplates = AppTemplate.find("select a from AppTemplate a inner join a.tags t where UPPER(t.tagId) = UPPER(?)", tagId).fetch();
            renderJSON(appTemplates);
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
            List<Tag> tags = Tag.findAll();
            renderJSON(tags);
        }
        else {
            error("Only requests that accept json are supported");
        }

    }

}
