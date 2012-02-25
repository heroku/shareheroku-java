package helpers;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import play.data.validation.Check;

public class CloneableAppCheck extends Check {

    @Override
    public boolean isSatisfied(Object validatedObject, Object value) {
        if ((value == null) || (value.toString().length() == 0)) {
            return false;
        }

        setMessage("validation.cloneableappcheck", System.getenv("HEROKU_USERNAME"));

        if (System.getenv("HEROKU_API_KEY") != null) {
            HerokuAPI herokuAPI = new HerokuAPI(System.getenv("HEROKU_API_KEY"));

            App app = herokuAPI.getApp(value.toString());

            if (app == null) {
                return false;
            }
        }

        return true;
    }
}
