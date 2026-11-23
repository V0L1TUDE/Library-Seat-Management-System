package com.library.seatmgmt;

import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test validating the Grace Period and No-Show Detection Logic.
 */
public class NoShowScannerTest {

    @Test
    @DisplayName("Verify Grace Period Boundaries for Early Arrival and No-Show Expiration")
    void testGracePeriodAndNoShowLogic() {
        LocalDate today = LocalDate.now();
        LocalTime slotStart = LocalTime.of(10, 0); // Slot starts at 10:00 AM
        LocalTime slotEnd = LocalTime.of(12, 0);

        Booking booking = new Booking(1, 1, 1, today, slotStart, slotEnd,
                BookingStatus.RESERVED, null, null, null, null);

        // Case 1: Student arrives too early (09:30 AM -> 30 mins before 10:00 AM)
        LocalDateTime earlyArrival = LocalDateTime.of(today, LocalTime.of(9, 30));
        assertFalse(booking.isEligibleForCheckIn(earlyArrival),
                "Should not be eligible for check-in more than 15 minutes before slot");
        assertFalse(booking.isNoShow(earlyArrival), "Cannot be marked as no-show before slot begins");

        // Case 2: Student arrives within early check-in window (09:50 AM -> 10 mins before 10:00 AM)
        LocalDateTime validEarlyArrival = LocalDateTime.of(today, LocalTime.of(9, 50));
        assertTrue(booking.isEligibleForCheckIn(validEarlyArrival),
                "Should be eligible for check-in within the 15-minute early arrival window");

        // Case 3: Student arrives within grace period (10:10 AM -> 10 mins after 10:00 AM)
        LocalDateTime withinGracePeriod = LocalDateTime.of(today, LocalTime.of(10, 10));
        assertTrue(booking.isEligibleForCheckIn(withinGracePeriod),
                "Should be eligible for check-in during the 15-minute grace period");
        assertFalse(booking.isNoShow(withinGracePeriod),
                "Should not be marked as no-show while still within 15-minute grace period");

        // Case 4: Student fails to arrive past grace period (10:16 AM -> 16 mins after 10:00 AM)
        LocalDateTime pastGraceDeadline = LocalDateTime.of(today, LocalTime.of(10, 16));
        assertFalse(booking.isEligibleForCheckIn(pastGraceDeadline),
                "Check-in window should close once grace period expires");
        assertTrue(booking.isNoShow(pastGraceDeadline),
                "Must be flagged as NO_SHOW once 15 minutes past start time without check-in");
    }
}
