package controllers;

public class SecurityAdmin extends Secure.Security {

    static boolean authenticate(String username, String password) {
        if ((System.getenv("ADMIN_USERNAME") == null) || (System.getenv("ADMIN_PASSWORD") == null)) {
            return true;
        }
        else {
            return ( (username.equals(System.getenv("ADMIN_USERNAME"))) && (password.equals(System.getenv("ADMIN_PASSWORD"))) );
        }
    }
}
