package service;

import app.Session;
import dao.UserDAO;
import exception.AuthenticationException;
import model.AccountStatus;
import model.User;
import util.PasswordUtil;

/**
 * Login and logout. The UI calls this; it never talks to the DAO directly.
 */
public class AuthenticationService {

    private final UserDAO userDAO;

    public AuthenticationService() {
        this(new UserDAO());
    }

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String email, char[] password) throws AuthenticationException {
        if (email == null || email.isBlank() || password == null || password.length == 0) {
            throw new AuthenticationException("Please enter your email and password.");
        }

        // Same message for "no such email" and "wrong password" so the
        // login screen does not reveal which emails are registered.
        User user = userDAO.findByEmail(email.trim().toLowerCase())
                .filter(u -> PasswordUtil.matches(new String(password), u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationException("Incorrect email or password."));

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new AuthenticationException("This account is suspended. Please contact the administrator.");
        }

        Session.login(user);
        return user;
    }

    public void logout() {
        Session.logout();
    }
}
