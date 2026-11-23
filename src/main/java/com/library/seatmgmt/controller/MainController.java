package com.library.seatmgmt.controller;

import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.UserPreferences;
import com.library.seatmgmt.service.AuthService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Main Controller orchestrating application navigation, sidebar states,
 * dynamic screen swaps, and real-time Dark/Light theme switching.
 */
public class MainController {

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavSeatMap;
    @FXML private Button btnNavMyBookings;
    @FXML private Button btnNavReports;
    @FXML private Button btnNavAdmin;
    @FXML private Button btnThemeToggle;

    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;
    @FXML private Label lblCurrentDate;
    @FXML private Label lblUserFullName;
    @FXML private Label lblUserRolePill;

    @FXML private StackPane contentArea;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user != null) {
            lblUserFullName.setText(user.getFullName());
            lblUserRolePill.setText(user.getRole().name());

            // If user is Student, hide admin management button
            if (user.isStudent()) {
                btnNavAdmin.setVisible(false);
                btnNavAdmin.setManaged(false);
            }
        }

        // Format today's date
        lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        // Update Theme Button label
        updateThemeButtonLabel();

        // Load Default Dashboard view
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveNav(btnNavDashboard);
        lblPageTitle.setText("Dashboard Overview");
        lblPageSubtitle.setText("Live library seating metrics and your recent bookings");
        loadView("/fxml/student_dashboard.fxml");
    }

    @FXML
    public void showSeatMap() {
        setActiveNav(btnNavSeatMap);
        lblPageTitle.setText("Interactive Seat Map");
        lblPageSubtitle.setText("Select your preferred study zone and reserve an available seat");
        loadView("/fxml/seat_map.fxml");
    }

    @FXML
    public void showMyBookings() {
        setActiveNav(btnNavMyBookings);
        lblPageTitle.setText("My Bookings");
        lblPageSubtitle.setText("Manage your upcoming, active, and past reservations");
        loadView("/fxml/my_bookings.fxml");
    }

    @FXML
    public void showReports() {
        setActiveNav(btnNavReports);
        lblPageTitle.setText("Reports & Analytics");
        lblPageSubtitle.setText("Seat utilization insights, peak hours, and CSV export");
        loadView("/fxml/reports.fxml");
    }

    @FXML
    public void showAdminView() {
        setActiveNav(btnNavAdmin);
        lblPageTitle.setText("Administrative Seat Control");
        lblPageSubtitle.setText("Manage zones, toggle maintenance, and inspect live capacity");
        loadView("/fxml/admin_dashboard.fxml");
    }

    @FXML
    public void toggleTheme() {
        UserPreferences prefs = SessionContext.getInstance().getUserPreferences();
        boolean newThemeIsDark = !prefs.isDarkModeEnabled();
        prefs.setDarkModeEnabled(newThemeIsDark);
        SessionContext.getInstance().saveUserPreferences(); // Persist to disk

        // Apply new stylesheet to active Scene
        Scene scene = contentArea.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource(newThemeIsDark ? "/css/dark.css" : "/css/main.css").toExternalForm());
        }

        updateThemeButtonLabel();
    }

    @FXML
    public void handleLogout() {
        authService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(root, 960, 620);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Smart Library Seat Management System - Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            LoggerUtil.severe("Failed to load login screen on logout", e);
        }
    }

    private void updateThemeButtonLabel() {
        boolean isDark = SessionContext.getInstance().getUserPreferences().isDarkModeEnabled();
        btnThemeToggle.setText(isDark ? "☀️  Light Mode" : "🌙  Dark Mode");
    }

    private void setActiveNav(Button activeButton) {
        btnNavDashboard.getStyleClass().remove("active");
        btnNavSeatMap.getStyleClass().remove("active");
        btnNavMyBookings.getStyleClass().remove("active");
        btnNavReports.getStyleClass().remove("active");
        btnNavAdmin.getStyleClass().remove("active");

        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            LoggerUtil.severe("Failed to load view: " + fxmlPath, e);
            Label errLabel = new Label("Failed to load screen: " + fxmlPath + "\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().setAll(errLabel);
        }
    }
}
