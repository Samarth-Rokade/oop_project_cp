package app;

import model.User;

import java.util.Optional;

/**
 * Holds the user who is currently logged in to this desktop app.
 */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }
}
