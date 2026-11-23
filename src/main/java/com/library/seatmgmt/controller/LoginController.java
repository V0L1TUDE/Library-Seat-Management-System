package com.library.seatmgmt.controller;

import com.library.seatmgmt.exception.AuthenticationException;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.service.AuthService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import com.library.seatmgmt.util.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the modern Login View.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Preload remembered username from serialized user preferences
        String remembered = SessionContext.getInstance().getUserPreferences().getRememberedUsername();
        if (remembered != null && !remembered.isEmpty()) {
            txtUsername.setText(remembered);
            Platform.runLater(() -> txtPassword.requestFocus());
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        lblError.setVisible(false);

        try {
            User user = authService.login(username, password);
            navigateToMainShell(user);
        } catch (AuthenticationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            LoggerUtil.severe("Unexpected error during login", e);
            showError("System error connecting to database. Please verify MySQL service is running.");
        }
    }

    @FXML
    public void fillStudentCredentials() {
        txtUsername.setText("student1");
        txtPassword.setText("password123");
        lblError.setVisible(false);
    }

    @FXML
    public void fillAdminCredentials() {
        txtUsername.setText("admin");
        txtPassword.setText("password123");
        lblError.setVisible(false);
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private void navigateToMainShell(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root, 1180, 740);

            // Apply active theme (Light or Dark)
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.setTitle("Smart Library Seat Management System - " + user.getRoleDisplayName());
            stage.centerOnScreen();
        } catch (IOException e) {
            LoggerUtil.severe("Failed to load main layout view", e);
            showError("Failed to load application shell.");
        }
    }
}
