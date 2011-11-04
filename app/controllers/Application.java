package controllers;

import helpers.HerokuAppSharingHelper;
import models.AppMetadata;
import play.*;
import play.mvc.*;
import play.data.validation.Error;

import java.util.*;

public class Application extends Controller {

    public static void index() {
        render();
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
            
            try {
                HerokuAppSharingHelper herokuAppSharingHelper = new HerokuAppSharingHelper();
                AppMetadata appMetadata = herokuAppSharingHelper.shareApp(emailAddress, gitUrl);
                
                Map<String, AppMetadata> result = new HashMap<String, AppMetadata>();
                result.put("result", appMetadata);
                renderJSON(result);
            }
            catch (RuntimeException e) {
                Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
                errors.put("error", new HashMap<String, String>());
                errors.get("error").put("shareApp", e.getMessage());
                renderJSON(errors);
            }
            
        }
    }

}