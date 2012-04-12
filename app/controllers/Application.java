package controllers;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.google.gson.Gson;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import helpers.EmailHelper;
import play.libs.F;
import play.mvc.*;
import play.data.validation.Error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Application extends Controller {

    private static final AnalyticsConfigData config = new AnalyticsConfigData("UA-26859570-1");

    public static void index() {
        render();
    }

    public static void shareApp(String emailAddress, String appId) {
        validation.email(emailAddress);
        validation.minSize(appId, 1);

        Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
        errors.put("error", new HashMap<String, String>());

        if(validation.hasErrors()) {
            for (Error error : validation.errors()) {
                errors.get("error").put(error.getKey(), error.message());
            }
            renderJSON(errors);
        }
        else {
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, JGoogleAnalyticsTracker.GoogleAnalyticsVersion.V_4_7_2);
            tracker.trackEvent("app", "shareApp", appId);

            try {

                HerokuAPI herokuAPI = new HerokuAPI(System.getenv("HEROKU_API_KEY"));

                App app = herokuAPI.getConnection().execute(new AppTemplateCreate(appId), System.getenv("HEROKU_API_KEY"));

                if (!app.getCreateStatus().equals("complete")) {
                    errors.get("error").put("shareApp", "Could not create the Heroku app");
                    renderJSON(errors);
                }

                // share the app with the provided email
                herokuAPI.addCollaborator(app.getName(), emailAddress);

                // transfer the app to the provided email
                herokuAPI.transferApp(app.getName(), emailAddress);

                // remove ${HEROKU_USERNAME} as collaborator
                herokuAPI.removeCollaborator(app.getName(), System.getenv("HEROKU_USERNAME"));

                Map<String, App> result = new HashMap<String, App>();
                result.put("result", app);

                renderJSON(result);
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
                renderJSON(errors);
            }
            
        }
    }

}
