package exception;

/**
 * Unchecked wrapper for database problems, so DAO callers are not forced
 * to handle SQLException everywhere. The original cause is kept.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
