package com.library.seatmgmt;

import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test validating File Handling and CSV Report Export.
 * Demonstrates syllabus File I/O requirement.
 */
public class FileUtilTest {

    @Test
    @DisplayName("Verify CSV Report Generation, Header Formatting, and Record Writing")
    void testExportBookingsToCsv() throws IOException {
        String testCsvPath = "target/test_reports/bookings_export_test.csv";

        List<Booking> bookings = new ArrayList<>();
        Booking b1 = new Booking(1, 5, LocalDate.of(2026, 11, 23),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
        b1.setId(101);
        b1.setStudentName("Alex Rivera");
        b1.setStudentIdNumber("STU-2024-001");
        b1.setSeatNumber("A-01");
        b1.setZoneName("Silent Study Gallery");
        b1.setStatus(BookingStatus.COMPLETED);
        bookings.add(b1);

        Booking b2 = new Booking(2, 8, LocalDate.of(2026, 11, 23),
                LocalTime.of(14, 0), LocalTime.of(16, 0));
        b2.setId(102);
        b2.setStudentName("Emily Chen");
        b2.setStudentIdNumber("STU-2024-002");
        b2.setSeatNumber("B-04");
        b2.setZoneName("Digital Research Lab");
        b2.setStatus(BookingStatus.RESERVED);
        bookings.add(b2);

        // Execute export
        boolean exported = FileUtil.exportBookingsToCsv(bookings, testCsvPath);
        assertTrue(exported, "CSV export should return true on success");

        File csvFile = new File(testCsvPath);
        assertTrue(csvFile.exists(), "CSV output file must exist on disk");

        // Read and verify contents line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String header = reader.readLine();
            assertNotNull(header);
            assertTrue(header.contains("Booking_ID") && header.contains("Seat_Number") && header.contains("Status"));

            String line1 = reader.readLine();
            assertNotNull(line1);
            assertTrue(line1.contains("101") && line1.contains("Alex Rivera") && line1.contains("A-01"));

            String line2 = reader.readLine();
            assertNotNull(line2);
            assertTrue(line2.contains("102") && line2.contains("Emily Chen") && line2.contains("B-04"));
        }

        // Clean up
        csvFile.delete();
    }
}
