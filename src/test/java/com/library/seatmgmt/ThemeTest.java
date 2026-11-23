package com.library.seatmgmt;

import com.library.seatmgmt.model.UserPreferences;
import com.library.seatmgmt.util.SessionContext;
import com.library.seatmgmt.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX runtime already started
        }
    }

    @Test
    void testThemeManagerApplyAndToggle() {
        Parent root = new javafx.scene.layout.StackPane();
        Scene scene = new Scene(root, 800, 600);

        // Test Light Theme
        ThemeManager.applyTheme(scene, false);
        assertEquals(1, scene.getStylesheets().size(), "Light mode should have 1 stylesheet (main.css)");
        assertTrue(scene.getStylesheets().get(0).contains("main.css"), "Light mode must include main.css");

        // Test Dark Theme
        ThemeManager.applyTheme(scene, true);
        assertEquals(2, scene.getStylesheets().size(), "Dark mode should have 2 stylesheets (main.css + dark.css)");
        assertTrue(scene.getStylesheets().get(0).contains("main.css"), "Dark mode must include main.css as base");
        assertTrue(scene.getStylesheets().get(1).contains("dark.css"), "Dark mode must include dark.css as override");

        // Test Toggle
        UserPreferences prefs = SessionContext.getInstance().getUserPreferences();
        prefs.setDarkModeEnabled(false);

        boolean toggled = ThemeManager.toggleTheme(scene);
        assertTrue(toggled, "Toggle from light should produce dark mode");
        assertTrue(prefs.isDarkModeEnabled());
        assertEquals(2, scene.getStylesheets().size());

        boolean toggledBack = ThemeManager.toggleTheme(scene);
        assertFalse(toggledBack, "Toggle from dark should produce light mode");
        assertFalse(prefs.isDarkModeEnabled());
        assertEquals(1, scene.getStylesheets().size());
    }

    @Test
    void testFxmlRenderWithDarkMode() throws Exception {
        List<String> fxmlFiles = List.of(
                "/fxml/login.fxml",
                "/fxml/main_layout.fxml",
                "/fxml/student_dashboard.fxml",
                "/fxml/seat_map.fxml",
                "/fxml/my_bookings.fxml",
                "/fxml/admin_dashboard.fxml",
                "/fxml/reports.fxml"
        );

        for (String file : fxmlFiles) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(file));
            Parent root = loader.load();
            assertNotNull(root, "Root must not be null for " + file);

            Scene scene = new Scene(root, 1180, 740);
            ThemeManager.applyTheme(scene, true); // Apply dark mode
            assertEquals(2, scene.getStylesheets().size(), "Dark mode stylesheets must be present on " + file);
        }
    }
}
