package ui;

import exception.AuthenticationException;
import exception.DataAccessException;
import exception.ValidationException;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Consistent message boxes. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void showError(Component parent, Throwable error) {
        if (error instanceof ValidationException || error instanceof AuthenticationException) {
            JOptionPane.showMessageDialog(parent, error.getMessage(),
                    "Please check", JOptionPane.WARNING_MESSAGE);
        } else if (error instanceof DataAccessException) {
            error.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    error.getMessage() + "\n\nIs the database running?  (docker compose up -d)",
                    "Database error", JOptionPane.ERROR_MESSAGE);
        } else {
            error.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Something went wrong: " + error,
                    "Unexpected error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
