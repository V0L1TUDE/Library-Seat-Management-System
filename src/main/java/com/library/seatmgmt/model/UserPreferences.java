package com.library.seatmgmt.model;

import java.io.Serializable;

/**
 * Persisted user preferences across sessions.
 * Demonstrates Java Object Serialization.
 */
public class UserPreferences implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean darkModeEnabled;       // Default is false (Light mode default)
    private String rememberedUsername;     // For login convenience
    private Integer defaultZoneId;         // Preferred zone filter
    private boolean soundAlertsEnabled;    // Audio feedback toggle

    public UserPreferences() {
        this.darkModeEnabled = false;      // Light mode is default per design specification
        this.soundAlertsEnabled = true;
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public String getRememberedUsername() {
        return rememberedUsername;
    }

    public void setRememberedUsername(String rememberedUsername) {
        this.rememberedUsername = rememberedUsername;
    }

    public Integer getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(Integer defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }

    public boolean isSoundAlertsEnabled() {
        return soundAlertsEnabled;
    }

    public void setSoundAlertsEnabled(boolean soundAlertsEnabled) {
        this.soundAlertsEnabled = soundAlertsEnabled;
    }

    @Override
    public String toString() {
        return "UserPreferences [darkMode=" + darkModeEnabled + ", user=" + rememberedUsername + "]";
    }
}
