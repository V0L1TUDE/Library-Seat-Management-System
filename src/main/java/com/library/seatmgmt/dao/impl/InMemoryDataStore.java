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
 * when MySQL is unavailable (e.g. during isolated unit tests).
 * Mirrors the exact 4-floor x 20-seats-per-floor structure of schema.sql.
 */
public final class InMemoryDataStore {

    private static InMemoryDataStore instance;

    public final Map<Integer, User> users = new ConcurrentHashMap<>();
    public final Map<Integer, Floor> floors = new ConcurrentHashMap<>();
    public final Map<Integer, Zone> zones = new ConcurrentHashMap<>();
    public final Map<Integer, Seat> seats = new ConcurrentHashMap<>();
    public final Map<Integer, Booking> bookings = new ConcurrentHashMap<>();
    public final List<ActivityLog> activityLogs = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger userSeq = new AtomicInteger(10);
    private final AtomicInteger floorSeq = new AtomicInteger(4);
    private final AtomicInteger seatSeq = new AtomicInteger(0);
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

        // 2. Seed 4 Independent Floors (20 seats per floor = 80 seats)
        Floor f1 = new Floor(1, "Ground Floor — Section 1", 0,
                "Open General Study Hall & Quick Access Circulation Desks (Seats 1 to 20).", true, LocalDateTime.now());
        Floor f2 = new Floor(2, "First Floor — Section 2", 1,
                "Silent Reading Gallery & Individual Focused Study Pods (Seats 1 to 20).", true, LocalDateTime.now());
        Floor f3 = new Floor(3, "Second Floor — Section 3", 2,
                "Digital Research Lab Equipped with Desktop PC Terminals (Seats 1 to 20).", true, LocalDateTime.now());
        Floor f4 = new Floor(4, "Third Floor — Section 4", 3,
                "Collaborative Study Wing & Academic Project Tables (Seats 1 to 20).", true, LocalDateTime.now());

        floors.put(1, f1);
        floors.put(2, f2);
        floors.put(3, f3);
        floors.put(4, f4);

        // Keep legacy zones map populated for backward compatibility
        zones.put(1, new Zone(1, f1.getFloorName(), "FLOOR-0", f1.getDescription(), ZoneType.GENERAL, true, LocalDateTime.now()));
        zones.put(2, new Zone(2, f2.getFloorName(), "FLOOR-1", f2.getDescription(), ZoneType.QUIET, true, LocalDateTime.now()));
        zones.put(3, new Zone(3, f3.getFloorName(), "FLOOR-2", f3.getDescription(), ZoneType.COMPUTER, true, LocalDateTime.now()));
        zones.put(4, new Zone(4, f4.getFloorName(), "FLOOR-3", f4.getDescription(), ZoneType.COLLABORATIVE, true, LocalDateTime.now()));

        // 3. Auto-generate 20 seats per floor
        generateSeatsForFloor(f1, false);
        generateSeatsForFloor(f2, false);
        generateSeatsForFloor(f3, true);
        generateSeatsForFloor(f4, false);

        // Mark 2 sample maintenance seats matching schema.sql
        seats.values().stream()
                .filter(s -> s.getFloorId() == 1 && "S-19".equals(s.getSeatNumber()))
                .findFirst().ifPresent(s -> s.setStatus(SeatStatus.MAINTENANCE));
        seats.values().stream()
                .filter(s -> s.getFloorId() == 2 && "S-20".equals(s.getSeatNumber()))
                .findFirst().ifPresent(s -> s.setStatus(SeatStatus.MAINTENANCE));

        // 4. Seed sample booking for student1
        Booking demoBooking = new Booking(101, 2, 1, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                BookingStatus.RESERVED, null, null, LocalDateTime.now(), LocalDateTime.now());
        demoBooking.setStudentName("Alex Rivera");
        demoBooking.setStudentIdNumber("STU-2024-001");
        demoBooking.setSeatNumber("S-01");
        demoBooking.setFloorId(1);
        demoBooking.setFloorName(f1.getFloorName());
        demoBooking.setFloorNumber(f1.getFloorNumber());
        bookings.put(101, demoBooking);

        // 5. Seed Activity Logs
        activityLogs.add(new ActivityLog(1, 1, "SYSTEM_INIT", "Floor-wise catalog initialized (4 floors, 80 seats).", LocalDateTime.now()));
        activityLogs.add(new ActivityLog(2, 2, "BOOKING_CREATED", "Demo booking #101 created for Ground Floor — Section 1, Seat S-01.", LocalDateTime.now()));
    }

    public synchronized void generateSeatsForFloor(Floor floor, boolean defaultPc) {
        long existing = seats.values().stream().filter(s -> s.getFloorId() == floor.getFloorId()).count();
        if (existing >= Floor.SEATS_PER_FLOOR) {
            return;
        }
        for (int i = 1; i <= Floor.SEATS_PER_FLOOR; i++) {
            String seatNum = String.format("S-%02d", i);
            boolean exists = seats.values().stream()
                    .anyMatch(s -> s.getFloorId() == floor.getFloorId() && seatNum.equalsIgnoreCase(s.getSeatNumber()));
            if (exists) continue;

            boolean hasPower = i <= 16;
            boolean nearWindow = (i == 1 || i == 5 || i == 6 || i == 10 || i == 11 || i == 15 || i == 16 || i == 20);
            boolean hasPc = defaultPc || i <= 4;

            int id = seatSeq.incrementAndGet();
            Seat s = new Seat(id, floor.getFloorId(), seatNum, hasPower, nearWindow, hasPc, SeatStatus.AVAILABLE, LocalDateTime.now());
            s.setFloorName(floor.getFloorName());
            s.setFloorNumber(floor.getFloorNumber());
            seats.put(id, s);
        }
    }

    public int nextUserId() {
        return userSeq.incrementAndGet();
    }

    public int nextFloorId() {
        return floorSeq.incrementAndGet();
    }

    public int nextSeatId() {
        return seatSeq.incrementAndGet();
    }

    public int nextBookingId() {
        return bookingSeq.incrementAndGet();
    }
}
