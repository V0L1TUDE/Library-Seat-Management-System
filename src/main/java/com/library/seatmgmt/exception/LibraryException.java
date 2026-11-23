package com.library.seatmgmt.exception;

/**
 * Root checked exception for all business logic failures in the Library Management System.
 * Demonstrates custom exception hierarchy.
 */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
