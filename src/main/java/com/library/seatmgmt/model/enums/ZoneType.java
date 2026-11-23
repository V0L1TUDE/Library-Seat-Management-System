package com.library.seatmgmt.model.enums;

/**
 * Functional category of a library zone.
 */
public enum ZoneType {
    QUIET("Quiet Study"),
    COMPUTER("Digital Research / PC"),
    COLLABORATIVE("Group Discussion"),
    GENERAL("General Reading");

    private final String displayName;

    ZoneType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
