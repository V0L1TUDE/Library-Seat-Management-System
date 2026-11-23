package com.library.seatmgmt.util;

import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.UserPreferences;

/**
 * Singleton managing the active authenticated session and in-memory application context.
 */
public final class SessionContext {

    private static final String PREF_FILE_PATH = "data/user_preferences.ser";
    private static SessionContext instance;

    private User currentUser;
    private UserPreferences userPreferences;

    private SessionContext() {
        // Attempt to load saved user preferences via Serialization
        this.userPreferences = SerializationUtil.deserialize(PREF_FILE_PATH, UserPreferences.class);
        if (this.userPreferences == null) {
            this.userPreferences = new UserPreferences(); // Fallback defaults (Light mode)
        }
    }

    public static synchronized SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isAuthenticated() {
        return this.currentUser != null;
    }

    public void clearSession() {
        this.currentUser = null;
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public void saveUserPreferences() {
        SerializationUtil.serialize(this.userPreferences, PREF_FILE_PATH);
    }
}
