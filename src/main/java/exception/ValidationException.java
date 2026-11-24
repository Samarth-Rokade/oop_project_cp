package exception;

/**
 * A business rule or input check failed. The message is written for the
 * user and can be shown directly in the UI.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
