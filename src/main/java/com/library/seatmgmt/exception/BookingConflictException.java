package com.library.seatmgmt.exception;

/**
 * Thrown when a student attempts to reserve a seat that overlaps with another active booking.
 */
public class BookingConflictException extends LibraryException {
    public BookingConflictException(String message) {
        super(message);
    }
}
