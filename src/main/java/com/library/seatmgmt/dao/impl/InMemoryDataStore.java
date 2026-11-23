package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.model.*;
import com.library.seatmgmt.model.enums.*;
import com.library.seatmgmt.util.PasswordUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe In-Memory Data Store providing graceful offline fallback
 * when MySQL is offline or undergoing initial credential configuration.
 * Contains the exact same seed data as schema.sql.
 */
public final class InMemoryDataStore {

    private static InMemoryDataStore instance;

    public final Map<Integer, User> users = new ConcurrentHashMap<>();
    public final Map<Integer, Zone> zones = new ConcurrentHashMap<>();
    public final Map<Integer, Seat> seats = new ConcurrentHashMap<>();
    public final Map<Integer, Booking> bookings = new ConcurrentHashMap<>();
    public final List<ActivityLog> activityLogs = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger userSeq = new AtomicInteger(10);
    private final AtomicInteger bookingSeq = new AtomicInteger(100);

    private InMemoryDataStore() {
        seedInitialData();
    }

    public static synchronized InMemoryDataStore getInstance() {
        if (instance == null) {
            instance = new InMemoryDataStore();
        }
        return instance;
    }

    private void seedInitialData() {
        String defaultHash = PasswordUtil.hashPassword("password123");

        // 1. Seed Users
        Admin admin = new Admin(1, "admin", defaultHash, "Dr. Sarah Jenkins", "admin@library.edu",
                LocalDateTime.now(), "Library Administration");
        users.put(1, admin);

        Student student1 = new Student(2, "student1", defaultHash, "Alex Rivera", "alex@student.edu",
                LocalDateTime.now(), "STU-2024-001", "Computer Science", "555-0101");
        users.put(2, student1);

        Student student2 = new Student(3, "student2", defaultHash, "Emily Chen", "emily@student.edu",
                LocalDateTime.now(), "STU-2024-002", "Electrical Engineering", "555-0102");
        users.put(3, student2);

        // 2. Seed Zones
        Zone z1 = new Zone(1, "Silent Study Gallery", "ZONE-A", "Absolute silence zone with individual reading pods.", ZoneType.QUIET, true, LocalDateTime.now());
        Zone z2 = new Zone(2, "Digital Research Lab", "ZONE-B", "Equipped with PCs and high-speed research terminals.", ZoneType.COMPUTER, true, LocalDateTime.now());
        Zone z3 = new Zone(3, "Discussion & Group Hub", "ZONE-C", "Collaborative area for group study and projects.", ZoneType.COLLABORATIVE, true, LocalDateTime.now());
        Zone z4 = new Zone(4, "Main Hall General Seating", "ZONE-D", "Open layout tables near central circulation desk.", ZoneType.GENERAL, true, LocalDateTime.now());

        zones.put(1, z1);
        zones.put(2, z2);
        zones.put(3, z3);
        zones.put(4, z4);

        // 3. Seed Seats
        addSeat(1, 1, "A-01", true, true, false, z1);
        addSeat(2, 1, "A-02", true, true, false, z1);
        addSeat(3, 1, "A-03", true, false, false, z1);
        addSeat(4, 1, "A-04", true, false, false, z1);
        addSeat(5, 1, "A-05", true, true, false, z1);

        addSeat(6, 2, "B-01", true, false, true, z2);
        addSeat(7, 2, "B-02", true, false, true, z2);
        addSeat(8, 2, "B-03", true, false, true, z2);
        addSeat(9, 2, "B-04", true, true, true, z2);

        addSeat(10, 3, "C-01", true, true, false, z3);
        addSeat(11, 3, "C-02", true, false, false, z3);
        addSeat(12, 3, "C-03", false, false, false, z3);

        addSeat(13, 4, "D-01", false, false, false, z4);
        addSeat(14, 4, "D-02", false, false, false, z4);
        addSeat(15, 4, "D-03", true, true, false, z4);

        // 4. Seed initial active booking for student1 (today at 10 AM)
        Booking demoBooking = new Booking(101, 2, 1, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                BookingStatus.RESERVED, null, null, LocalDateTime.now(), LocalDateTime.now());
        demoBooking.setStudentName("Alex Rivera");
        demoBooking.setStudentIdNumber("STU-2024-001");
        demoBooking.setSeatNumber("A-01");
        demoBooking.setZoneName("Silent Study Gallery");
        bookings.put(101, demoBooking);

        // 5. Seed Activity Logs
        activityLogs.add(new ActivityLog(1, 1, "SYSTEM_INIT", "Database and seed catalog initialized.", LocalDateTime.now()));
        activityLogs.add(new ActivityLog(2, 2, "BOOKING_CREATED", "Demo booking #101 created for Seat A-01.", LocalDateTime.now()));
    }

    private void addSeat(int id, int zoneId, String num, boolean power, boolean window, boolean pc, Zone z) {
        Seat s = new Seat(id, zoneId, num, power, window, pc, SeatStatus.AVAILABLE, LocalDateTime.now());
        s.setZoneName(z.getName());
        s.setZoneCode(z.getCode());
        seats.put(id, s);
    }

    public int nextUserId() {
        return userSeq.incrementAndGet();
    }

    public int nextBookingId() {
        return bookingSeq.incrementAndGet();
    }
}
