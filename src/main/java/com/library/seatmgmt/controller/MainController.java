package com.library.seatmgmt.controller;

import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.UserPreferences;
import com.library.seatmgmt.service.AuthService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import com.library.seatmgmt.util.ThemeManager;
import javafx.application.Platform;
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
import java.util.Locale;

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

            // Role-based sidebar navigation visibility
            if (user.isStudent()) {
                btnNavReports.setVisible(false);
                btnNavReports.setManaged(false);
                btnNavAdmin.setVisible(false);
                btnNavAdmin.setManaged(false);
            } else if (user.isAdmin()) {
                btnNavMyBookings.setVisible(false);
                btnNavMyBookings.setManaged(false);
            }
        }

        // Format today's date
        lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)));

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
        lblPageTitle.setText("Floor-Wise Seat Map");
        lblPageSubtitle.setText("Select a library floor (20 seats per section) and reserve an available seat");
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
        lblPageSubtitle.setText("Floor-wise utilization reports, peak hours, and CSV export");
        loadView("/fxml/reports.fxml");
    }

    @FXML
    public void showAdminView() {
        setActiveNav(btnNavAdmin);
        lblPageTitle.setText("Floor & Seat Administration");
        lblPageSubtitle.setText("Manage library floors (+20 seats), seat inventory, and monitor student bookings");
        loadView("/fxml/admin_dashboard.fxml");
    }

    @FXML
    public void toggleTheme() {
        Scene scene = contentArea.getScene();
        if (scene != null) {
            ThemeManager.toggleTheme(scene);
        } else {
            UserPreferences prefs = SessionContext.getInstance().getUserPreferences();
            prefs.setDarkModeEnabled(!prefs.isDarkModeEnabled());
            SessionContext.getInstance().saveUserPreferences();
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
            ThemeManager.applyTheme(scene);

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
