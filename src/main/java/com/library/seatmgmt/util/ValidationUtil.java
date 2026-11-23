package com.library.seatmgmt.util;

import com.library.seatmgmt.exception.InvalidBookingException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * Utility for performing robust validation across models and UI input forms.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private static final int MIN_BOOKING_MINUTES = 30;
    private static final int MAX_BOOKING_MINUTES = 360; // 6 hours maximum per booking

    private ValidationUtil() {
        // Prevent instantiation
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validates that requested booking date and time meet library rules.
     * Throws InvalidBookingException with informative error messages.
     */
    public static void validateBookingSlot(LocalDate date, LocalTime start, LocalTime end)
            throws InvalidBookingException {

        if (date == null || start == null || end == null) {
            throw new InvalidBookingException("Date, start time, and end time are required.");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 1. Cannot book for a past date
        if (date.isBefore(today)) {
            throw new InvalidBookingException("Cannot make a reservation for a past date.");
        }

        // 2. Cannot book more than 7 days in advance
        if (date.isAfter(today.plusDays(7))) {
            throw new InvalidBookingException("Reservations are only permitted up to 7 days in advance.");
        }

        // 3. If booking for today, slot start cannot be in the past
        if (date.isEqual(today) && start.isBefore(now.minusMinutes(5))) {
            throw new InvalidBookingException("Start time cannot be in the past.");
        }

        // 4. Start time must be before end time
        if (!start.isBefore(end)) {
            throw new InvalidBookingException("End time must be strictly after start time.");
        }

        // 5. Check duration constraints
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < MIN_BOOKING_MINUTES) {
            throw new InvalidBookingException("Minimum booking duration is " + MIN_BOOKING_MINUTES + " minutes.");
        }

        if (minutes > MAX_BOOKING_MINUTES) {
            throw new InvalidBookingException("Maximum booking duration is " + (MAX_BOOKING_MINUTES / 60) + " hours.");
        }
    }
}
