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

    private DBConnection() {
        loadProperties();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new DatabaseOperationException("Could not find configuration file: " + PROPERTIES_FILE);
            }
            props.load(in);

            this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/library_seat_db");
            this.username = props.getProperty("db.username", "root");
            this.password = props.getProperty("db.password", "root");
            this.driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            this.timeout = Integer.parseInt(props.getProperty("db.timeout", "5"));

            // Register driver class
            Class.forName(driver);
            LoggerUtil.info("Database driver loaded successfully: " + driver);
        } catch (IOException | ClassNotFoundException e) {
            LoggerUtil.severe("Failed to initialize database configuration", e);
            throw new DatabaseOperationException("Failed to initialize database driver or properties", e);
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
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LoggerUtil.warning("Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }
}
