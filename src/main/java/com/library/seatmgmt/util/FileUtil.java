package com.library.seatmgmt.util;

import com.library.seatmgmt.model.Booking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * File I/O Utility demonstrating file creation, writing, and CSV formatted reporting.
 * Fulfills syllabus File Handling requirement.
 */
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * Exports a collection of Booking records to a CSV file.
     */
    public static boolean exportBookingsToCsv(List<Booking> bookings, String targetFilePath) {
        if (bookings == null || targetFilePath == null) {
            return false;
        }

        File file = new File(targetFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write CSV Header
            writer.write("Booking_ID,Student_Name,Student_ID,Seat_Number,Floor,Date,Start_Time,End_Time,Status,Checked_In_At");
            writer.newLine();

            // Write Records
            DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Booking b : bookings) {
                String checkedInStr = (b.getCheckInTime() != null) ? b.getCheckInTime().format(tsFmt) : "N/A";
                String studentName = (b.getStudentName() != null) ? b.getStudentName() : "Student";
                String studentRoll = (b.getStudentIdNumber() != null) ? b.getStudentIdNumber() : "N/A";
                String floorName = (b.getFloorName() != null) ? b.getFloorName() : "Floor";

                String line = String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,\"%s\"",
                        b.getId(),
                        escapeCsv(studentName),
                        escapeCsv(studentRoll),
                        escapeCsv(b.getSeatNumber()),
                        escapeCsv(floorName),
                        b.getBookingDate(),
                        b.getStartTime(),
                        b.getEndTime(),
                        b.getStatus().name(),
                        checkedInStr
                );
                writer.write(line);
                writer.newLine();
            }

            writer.flush();
            LoggerUtil.info("CSV report exported successfully to: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            LoggerUtil.severe("Failed to export bookings CSV to: " + targetFilePath, e);
            return false;
        }
    }

    private static String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
