package com.library.seatmgmt;

import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Application Launcher for the Smart Library Seat Management System.
 * Connects to MySQL, loads serialized user preferences, and launches the JavaFX Desktop UI.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            LoggerUtil.info("Booting Smart Library Seat Management System...");

            // Test Database Connection Diagnostic
            boolean dbOnline = DBConnection.getInstance().testConnection();
            if (dbOnline) {
                LoggerUtil.info("Connected to MySQL Database: " + DBConnection.getInstance().getUrl());
            } else {
                LoggerUtil.warning("MySQL Database is currently unreachable. Launching in offline diagnostic mode.");
            }

            // Load Login Screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 960, 620);

            // Apply theme (Light default, Dark if saved in serialized preferences)
            boolean isDark = SessionContext.getInstance().getUserPreferences().isDarkModeEnabled();
            scene.getStylesheets().add(getClass().getResource(isDark ? "/css/dark.css" : "/css/main.css").toExternalForm());

            primaryStage.setTitle("Smart Library Seat Management System - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(620);
            primaryStage.show();

            LoggerUtil.info("JavaFX GUI desktop stage displayed successfully.");
        } catch (Exception e) {
            LoggerUtil.severe("Fatal error during JavaFX startup", e);
            System.err.println("Fatal error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
