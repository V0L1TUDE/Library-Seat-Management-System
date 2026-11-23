package com.library.seatmgmt.util;

import com.library.seatmgmt.model.UserPreferences;
import javafx.scene.Scene;

import java.net.URL;

/**
 * Central Theme Manager for toggling and applying Academic Light and Deep Forest Dark themes.
 * Coordinates with SessionContext and SerializationUtil for session persistence.
 */
public class ThemeManager {

    private static final String MAIN_CSS_PATH = "/css/main.css";
    private static final String DARK_CSS_PATH = "/css/dark.css";

    private ThemeManager() {}

    /**
     * Applies the currently persisted theme preference to the given Scene.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        boolean isDark = SessionContext.getInstance().getUserPreferences().isDarkModeEnabled();
        applyTheme(scene, isDark);
    }

    /**
     * Applies either Light or Dark theme to the specified Scene.
     * In Dark Mode, main.css provides base layout and dimensions, while dark.css provides the dark color palette.
     */
    public static void applyTheme(Scene scene, boolean isDark) {
        if (scene == null) return;

        scene.getStylesheets().clear();

        URL mainUrl = ThemeManager.class.getResource(MAIN_CSS_PATH);
        if (mainUrl != null) {
            scene.getStylesheets().add(mainUrl.toExternalForm());
        }

        if (isDark) {
            URL darkUrl = ThemeManager.class.getResource(DARK_CSS_PATH);
            if (darkUrl != null) {
                scene.getStylesheets().add(darkUrl.toExternalForm());
            }
        }
    }

    /**
     * Toggles between Light and Dark mode, updates UserPreferences, persists to disk, and updates the Scene.
     * @return the new darkMode state (true = Dark Mode active, false = Light Mode active)
     */
    public static boolean toggleTheme(Scene scene) {
        UserPreferences prefs = SessionContext.getInstance().getUserPreferences();
        boolean newThemeIsDark = !prefs.isDarkModeEnabled();
        prefs.setDarkModeEnabled(newThemeIsDark);
        SessionContext.getInstance().saveUserPreferences();

        applyTheme(scene, newThemeIsDark);
        LoggerUtil.info("Theme toggled to: " + (newThemeIsDark ? "Dark Mode" : "Light Mode"));
        return newThemeIsDark;
    }
}
