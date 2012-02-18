package controllers;

public class Security extends Secure.Security {

    static boolean authenticate(String username, String password) {
        return ( (username.equals(System.getenv("ADMIN_USERNAME"))) && (password.equals(System.getenv("ADMIN_PASSWORD"))) );
    }
}
