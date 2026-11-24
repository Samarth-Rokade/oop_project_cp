package config;

import app.Session;
import exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Loads database settings once and hands out JDBC connections.
 *
 * Lookup order for settings:
 *   1. db.properties          (project root - each developer's own copy, git-ignored)
 *   2. db.properties.example  (project root - committed, matches docker-compose.yml)
 *
 * Callers must close connections, ideally with try-with-resources.
 */
public final class DatabaseConfig {

    private static final String[] CANDIDATE_FILES = {"db.properties", "db.properties.example"};

    private static String url;
    private static String user;
    private static String password;

    private DatabaseConfig() {
        // utility class - no instances
    }

    private static synchronized void loadIfNeeded() {
        if (url != null) {
            return;
        }
        Properties props = new Properties();
        for (String name : CANDIDATE_FILES) {
            Path path = Path.of(name);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    props.load(in);
                    System.out.println("[DatabaseConfig] Using settings from " + path.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    throw new DataAccessException("Could not read " + name, e);
                }
            }
        }
        if (props.isEmpty()) {
            throw new DataAccessException(
                    "No db.properties found. Run the app from the project root folder, "
                            + "or copy db.properties.example to db.properties.", null);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password", "");
    }

    /**
     * Opens a new connection. If someone is logged in, their user id is
     * stored in the MySQL session variable {@code @app_user_id} so the
     * database triggers can record who made a change.
     */
    public static Connection getConnection() throws SQLException {
        loadIfNeeded();
        Connection connection = DriverManager.getConnection(url, user, password);
        Session.getCurrentUser().ifPresent(u -> {
            try (Statement st = connection.createStatement()) {
                st.execute("SET @app_user_id = " + u.getUserId());
            } catch (SQLException e) {
                throw new DataAccessException("Could not set session user", e);
            }
        });
        return connection;
    }
}
