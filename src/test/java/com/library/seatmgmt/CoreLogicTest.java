package com.library.seatmgmt;

import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.model.Admin;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Student;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.UserPreferences;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.model.enums.Role;
import com.library.seatmgmt.util.PasswordUtil;
import com.library.seatmgmt.util.SerializationUtil;
import com.library.seatmgmt.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite validating Core Java implementation, OOP principles,
 * booking conflict algorithm, validation rules, and object serialization.
 */
public class CoreLogicTest {

    @Test
    @DisplayName("Verify OOP Inheritance and Polymorphic method behavior")
    void testInheritanceAndPolymorphism() {
        User student = new Student(1, "alex", "hash123", "Alex Rivera", "alex@edu.com",
                null, "STU-001", "Computer Science", "555-0101");
        User admin = new Admin(2, "admin", "hash456", "Dr. Jenkins", "admin@edu.com",
                null, "Administration");

        assertEquals(Role.STUDENT, student.getRole());
        assertEquals(Role.ADMIN, admin.getRole());

        // Polymorphic calls to getRoleDisplayName()
        assertTrue(student.getRoleDisplayName().contains("Computer Science"));
        assertEquals("Library Administrator", admin.getRoleDisplayName());

        assertTrue(student.isStudent());
        assertFalse(student.isAdmin());
        assertTrue(admin.isAdmin());
    }

    @Test
    @DisplayName("Verify Booking Conflict Algorithm Across All Edge Cases")
    void testBookingConflictAlgorithm() {
        LocalDate today = LocalDate.now();
        LocalTime slotStart = LocalTime.of(10, 0); // 10:00 AM
        LocalTime slotEnd = LocalTime.of(12, 0);   // 12:00 PM

        Booking existingBooking = new Booking(1, 1, 1, today, slotStart, slotEnd,
                BookingStatus.RESERVED, null, null, null, null);

        // Case 1: Exact same slot -> CONFLICT
        assertTrue(existingBooking.isOverlappingWith(today, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                "Exact identical slot should conflict");

        // Case 2: Contained inside -> CONFLICT
        assertTrue(existingBooking.isOverlappingWith(today, LocalTime.of(10, 30), LocalTime.of(11, 30)),
                "Slot inside existing booking should conflict");

        // Case 3: Engulfing -> CONFLICT
        assertTrue(existingBooking.isOverlappingWith(today, LocalTime.of(9, 30), LocalTime.of(12, 30)),
                "Slot engulfing existing booking should conflict");

        // Case 4: Overlap left -> CONFLICT
        assertTrue(existingBooking.isOverlappingWith(today, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                "Slot overlapping left boundary should conflict");

        // Case 5: Overlap right -> CONFLICT
        assertTrue(existingBooking.isOverlappingWith(today, LocalTime.of(11, 30), LocalTime.of(12, 30)),
                "Slot overlapping right boundary should conflict");

        // Case 6: Back-to-Back (Immediate after: 12:00 to 14:00) -> NO CONFLICT
        assertFalse(existingBooking.isOverlappingWith(today, LocalTime.of(12, 0), LocalTime.of(14, 0)),
                "Back-to-back reservation (ending exactly at new start) must NOT conflict");

        // Case 7: Back-to-Back (Immediate before: 08:00 to 10:00) -> NO CONFLICT
        assertFalse(existingBooking.isOverlappingWith(today, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                "Back-to-back reservation (starting exactly at new end) must NOT conflict");

        // Case 8: Different date -> NO CONFLICT
        assertFalse(existingBooking.isOverlappingWith(today.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0)),
                "Same time on different date must NOT conflict");

        // Case 9: Inactive/Cancelled booking -> NO CONFLICT
        existingBooking.setStatus(BookingStatus.CANCELLED);
        assertFalse(existingBooking.isOverlappingWith(today, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                "Cancelled booking must NOT block new reservations");
    }

    @Test
    @DisplayName("Verify Validation Utility for Booking Slots and Email Formats")
    void testValidationUtility() {
        assertTrue(ValidationUtil.isValidEmail("alex@student.edu"));
        assertTrue(ValidationUtil.isValidEmail("john.doe@domain.co.in"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidEmail(null));

        LocalDate today = LocalDate.now();

        // End time before start time should throw InvalidBookingException
        assertThrows(InvalidBookingException.class, () ->
                ValidationUtil.validateBookingSlot(today, LocalTime.of(14, 0), LocalTime.of(12, 0)));

        // Duration less than 30 minutes should throw InvalidBookingException
        assertThrows(InvalidBookingException.class, () ->
                ValidationUtil.validateBookingSlot(today, LocalTime.of(14, 0), LocalTime.of(14, 15)));

        // Past date should throw InvalidBookingException
        assertThrows(InvalidBookingException.class, () ->
                ValidationUtil.validateBookingSlot(today.minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Verify Java Object Serialization and Deserialization for User Preferences")
    void testSerializationUtility() {
        String testFilePath = "target/test_preferences.ser";

        UserPreferences prefs = new UserPreferences();
        prefs.setDarkModeEnabled(true);
        prefs.setRememberedUsername("test_student");
        prefs.setDefaultZoneId(2);

        // Serialize object to disk
        boolean serialized = SerializationUtil.serialize(prefs, testFilePath);
        assertTrue(serialized, "Object serialization should succeed");

        File file = new File(testFilePath);
        assertTrue(file.exists(), "Serialized file must exist on disk");

        // Deserialize back
        UserPreferences restored = SerializationUtil.deserialize(testFilePath, UserPreferences.class);
        assertNotNull(restored, "Deserialized object must not be null");
        assertTrue(restored.isDarkModeEnabled(), "Restored preference should have dark mode enabled");
        assertEquals("test_student", restored.getRememberedUsername());
        assertEquals(2, restored.getDefaultZoneId());

        // Cleanup
        file.delete();
    }

    @Test
    @DisplayName("Verify SHA-256 Password Hashing and Verification")
    void testPasswordSecurity() {
        String rawPassword = "collegeSecretPassword2024!";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash must be 64 hexadecimal characters");

        assertTrue(PasswordUtil.verifyPassword(rawPassword, hash), "Password verification should pass");
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", hash), "Wrong password should fail");
    }
}
