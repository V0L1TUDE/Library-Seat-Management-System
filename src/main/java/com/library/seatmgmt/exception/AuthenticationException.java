package com.library.seatmgmt.exception;

/**
 * Thrown when user login or credential verification fails.
 */
public class AuthenticationException extends LibraryException {
    public AuthenticationException(String message) {
        super(message);
    }
}
