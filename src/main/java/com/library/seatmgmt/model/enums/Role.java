package com.library.seatmgmt.model.enums;

/**
 * Defines user authorization roles within the library system.
 */
public enum Role {
    STUDENT("Student"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
