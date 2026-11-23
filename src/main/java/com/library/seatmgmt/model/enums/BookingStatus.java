package com.library.seatmgmt.model.enums;

/**
 * Lifecycle status of a seat booking.
 */
public enum BookingStatus {
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines whether the booking holds active claim to the seat.
     */
    public boolean isActive() {
        return this == RESERVED || this == OCCUPIED;
    }
}
