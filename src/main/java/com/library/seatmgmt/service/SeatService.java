package com.library.seatmgmt.service;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.ZoneDAO;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.dao.impl.ZoneDAOImpl;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.SeatStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing library seating layout, zone distribution, and slot availability.
 */
public class SeatService {

    private final SeatDAO seatDAO;
    private final ZoneDAO zoneDAO;
    private final BookingDAO bookingDAO;

    public SeatService() {
        this(new SeatDAOImpl(), new ZoneDAOImpl(), new BookingDAOImpl());
    }

    public SeatService(SeatDAO seatDAO, ZoneDAO zoneDAO, BookingDAO bookingDAO) {
        this.seatDAO = seatDAO;
        this.zoneDAO = zoneDAO;
        this.bookingDAO = bookingDAO;
    }

    public List<Zone> getAllActiveZones() {
        return zoneDAO.findAll(true);
    }

    public List<Seat> getSeatsByZone(int zoneId) {
        return seatDAO.findByZoneId(zoneId);
    }

    /**
     * Filters and returns seats in a zone that have NO overlapping active bookings for the specified interval.
     */
    public List<Seat> getAvailableSeatsForSlot(int zoneId, LocalDate date, LocalTime start, LocalTime end) {
        List<Seat> allSeats = seatDAO.findByZoneId(zoneId);

        // Filter out seats currently in maintenance
        List<Seat> operationalSeats = allSeats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .toList();

        // Get all active bookings for this zone and date
        List<Booking> zoneBookings = bookingDAO.findByDateAndZone(date, zoneId);

        // Find IDs of seats that conflict with requested time slot
        Set<Integer> conflictingSeatIds = zoneBookings.stream()
                .filter(b -> b.isOverlappingWith(date, start, end))
                .map(Booking::getSeatId)
                .collect(Collectors.toSet());

        // Return only seats not in conflict
        return operationalSeats.stream()
                .filter(s -> !conflictingSeatIds.contains(s.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Determines the dynamic status of a seat for a specific date and time slot.
     * Returns: "AVAILABLE", "RESERVED", "OCCUPIED", or "MAINTENANCE"
     */
    public String getSeatStatusForSlot(Seat seat, LocalDate date, LocalTime start, LocalTime end) {
        if (seat.getStatus() == SeatStatus.MAINTENANCE) {
            return "MAINTENANCE";
        }

        List<Booking> conflicts = bookingDAO.findOverlappingBookings(seat.getId(), date, start, end);
        if (conflicts.isEmpty()) {
            return "AVAILABLE";
        }

        boolean anyOccupied = conflicts.stream().anyMatch(b -> b.getStatus().name().equals("OCCUPIED"));
        return anyOccupied ? "OCCUPIED" : "RESERVED";
    }

    public boolean updateSeatStatus(int seatId, SeatStatus status) {
        return seatDAO.updateStatus(seatId, status);
    }
}
