package com.library.seatmgmt.util;

import com.library.seatmgmt.exception.DatabaseOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-safe Database Connection Utility using Singleton and Factory principles.
 * Loads connection settings from config/db.properties.
 * Demonstrates JDBC driver loading, Connection management, and Resource Handling.
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "/config/db.properties";
    private static DBConnection instance;

    private String url;
    private String username;
    private String password;
    private String driver;
    private int timeout;
    private Boolean dbAvailable = null;

    private DBConnection() {
        loadProperties();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                LoggerUtil.warning("Could not find configuration file: " + PROPERTIES_FILE + ". Using defaults.");
                this.url = "jdbc:mysql://localhost:3306/library_seat_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                this.username = "root";
                this.password = "Pubg2arjan";
                this.driver = "com.mysql.cj.jdbc.Driver";
                this.timeout = 3;
                return;
            }
            props.load(in);

            this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/library_seat_db");
            this.username = props.getProperty("db.username", "root");
            this.password = props.getProperty("db.password", "root");
            this.driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            this.timeout = Integer.parseInt(props.getProperty("db.timeout", "3"));

            // Register driver class
            Class.forName(driver);
        } catch (IOException | ClassNotFoundException e) {
            LoggerUtil.warning("Failed to initialize database driver: " + e.getMessage());
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Obtains a new database connection from the driver manager.
     */
    public Connection getConnection() throws SQLException {
        DriverManager.setLoginTimeout(timeout);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Diagnostic test to check whether the MySQL database is reachable.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean ok = conn != null && !conn.isClosed();
            this.dbAvailable = ok;
            return ok;
        } catch (SQLException e) {
            this.dbAvailable = false;
            return false;
        }
    }

    /**
     * Returns true if MySQL is connected; otherwise false to allow seamless offline fallback.
     */
    public boolean isAvailable() {
        if (dbAvailable == null) {
            testConnection();
        }
        return Boolean.TRUE.equals(dbAvailable);
    }

    public void invalidateAvailability() {
        this.dbAvailable = null;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }
}
