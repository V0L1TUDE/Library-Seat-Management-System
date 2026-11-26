package com.library.seatmgmt.exception;

/**
 * Thrown when a requested seat does not exist or otherwise cannot be reserved.
 */
public class SeatUnavailableException extends LibraryException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
