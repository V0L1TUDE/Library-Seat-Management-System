package com.library.seatmgmt.model.enums;

/**
 * Physical operational status of a library seat.
 */
public enum SeatStatus {
    AVAILABLE("Available"),
    MAINTENANCE("Under Maintenance");

    private final String displayName;

    SeatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
