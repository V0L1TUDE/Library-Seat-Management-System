package com.library.seatmgmt.exception;

/**
 * Thrown when a seat is in maintenance mode or otherwise cannot be reserved.
 */
public class SeatUnavailableException extends LibraryException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
