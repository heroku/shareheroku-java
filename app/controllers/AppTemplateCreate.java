package controllers;

import com.heroku.api.App;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;
import com.heroku.api.request.app.AppCreate;

import static com.heroku.api.parser.Json.parse;

public class AppTemplateCreate extends AppCreate {

    private final String appId;

    public AppTemplateCreate(String appId) {
        super(new App());
        this.appId = appId;
    }

    @Override
    public String getBody() {
        return "app[stack]=cedar&app[template]=" + appId;
    }

    @Override
    public App getResponse(byte[] in, int code) {
        if (code == Http.Status.ACCEPTED.statusCode)
            return parse(in, AppCreate.class);
        else
            throw new RequestFailedException("Failed to create app", code, in);
    }
}