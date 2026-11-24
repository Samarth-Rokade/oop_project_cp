package util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt helpers. Plain-text passwords are never stored. */
public final class PasswordUtil {

    private static final int COST = 10;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
