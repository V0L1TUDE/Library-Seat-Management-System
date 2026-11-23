package com.library.seatmgmt.exception;

/**
 * Thrown when booking parameters violate rules (e.g., booking in past, end time before start time, duration too long).
 */
public class InvalidBookingException extends LibraryException {
    public InvalidBookingException(String message) {
        super(message);
    }
}
