package exception;

/** Thrown when login fails. The message is safe to show to the user. */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }
}
