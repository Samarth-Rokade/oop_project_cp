package exception;

/**
 * The database rejected a row because a UNIQUE value already exists
 * (for example an email address that is already registered).
 */
public class DuplicateEntryException extends DataAccessException {

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
