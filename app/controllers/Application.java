package controllers;

import play.mvc.*;

public class Application extends Controller {

    public static void index() {
        redirect("http://heroku.com/java");
    }

}
