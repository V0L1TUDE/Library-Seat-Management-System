package com.library.seatmgmt.exception;

/**
 * Unchecked runtime exception wrapping low-level database SQLExceptions to prevent SQL leakage into UI controllers.
 */
public class DatabaseOperationException extends RuntimeException {
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseOperationException(String message) {
        super(message);
    }
}
