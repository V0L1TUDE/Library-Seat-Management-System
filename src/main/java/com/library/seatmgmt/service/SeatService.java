package com.library.seatmgmt.service;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.FloorDAO;
import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.ZoneDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.dao.impl.FloorDAOImpl;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.dao.impl.ZoneDAOImpl;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing library floor sections, 20-seat-per-floor layout,
 * seat statuses, and slot availability.
 */
public class SeatService {

    private final FloorDAO floorDAO;
    private final SeatDAO seatDAO;
    private final ZoneDAO zoneDAO;
    private final BookingDAO bookingDAO;
    private final ActivityLogDAO logDAO;

    public SeatService() {
        this(new FloorDAOImpl(), new SeatDAOImpl(), new ZoneDAOImpl(), new BookingDAOImpl(), new ActivityLogDAOImpl());
    }

    public SeatService(SeatDAO seatDAO, ZoneDAO zoneDAO, BookingDAO bookingDAO) {
        this(new FloorDAOImpl(), seatDAO, zoneDAO, bookingDAO, new ActivityLogDAOImpl());
    }

    public SeatService(FloorDAO floorDAO, SeatDAO seatDAO, ZoneDAO zoneDAO, BookingDAO bookingDAO, ActivityLogDAO logDAO) {
        this.floorDAO = floorDAO;
        this.seatDAO = seatDAO;
        this.zoneDAO = zoneDAO;
        this.bookingDAO = bookingDAO;
        this.logDAO = logDAO;
    }

    public List<Floor> getAllActiveFloors() {
        return floorDAO.findAll(true);
    }

    public List<Floor> getAllFloors() {
        return floorDAO.findAll(false);
    }

    public List<Seat> getSeatsByFloor(int floorId) {
        return seatDAO.findByFloorId(floorId);
    }

    public List<Zone> getAllActiveZones() {
        return zoneDAO.findAll(true);
    }

    public List<Seat> getSeatsByZone(int zoneId) {
        return seatDAO.findByFloorId(zoneId);
    }

    /**
     * Adds a new library floor section and automatically provisions 20 unique seats.
     */
    public Floor addFloor(String floorName, int floorNumber, String description, Integer adminUserId)
            throws InvalidBookingException {
        if (!ValidationUtil.isNotEmpty(floorName)) {
            throw new InvalidBookingException("Floor name is required.");
        }
        String trimmedName = floorName.trim();
        if (floorNumber < 0 || floorNumber > 50) {
            throw new InvalidBookingException("Floor number must be between 0 (Ground) and 50.");
        }
        if (floorDAO.findByFloorNumber(floorNumber).isPresent()) {
            throw new InvalidBookingException("Floor number " + floorNumber + " already exists.");
        }
        if (floorDAO.findByFloorName(trimmedName).isPresent()) {
            throw new InvalidBookingException("A floor named '" + trimmedName + "' already exists.");
        }

        Floor floor = new Floor(trimmedName, floorNumber, description != null ? description.trim() : "");
        if (!floorDAO.save(floor)) {
            throw new InvalidBookingException("Failed to create floor in the database.");
        }

        logDAO.save(new ActivityLog(adminUserId, "FLOOR_CREATED",
                "Added floor '" + floor.getFloorName() + "' (Floor #" + floorNumber + ") with 20 seats."));
        LoggerUtil.info("Added floor '" + floor.getFloorName() + "' with 20 seats.");
        return floor;
    }

    /**
     * Updates a physical seat's operational status (AVAILABLE or MAINTENANCE).
     */
    public boolean updateSeatStatus(int seatId, SeatStatus status, Integer adminUserId) {
        boolean updated = seatDAO.updateStatus(seatId, status);
        if (updated) {
            seatDAO.findById(seatId).ifPresent(seat -> logDAO.save(new ActivityLog(adminUserId, "SEAT_STATUS_UPDATED",
                    "Changed " + seat.getFloorName() + " Seat " + seat.getSeatNumber() + " to " + status.name())));
        }
        return updated;
    }

    /**
     * Returns live floor-wise occupancy breakdown for the specified date and time slot.
     */
    public List<Floor> getFloorOccupancySummary(LocalDate date, LocalTime start, LocalTime end) {
        return floorDAO.getFloorOccupancySummary(date, start, end);
    }

    /**
     * Filters and returns operational seats on a floor that have NO overlapping active bookings.
     */
    public List<Seat> getAvailableSeatsForSlot(int floorId, LocalDate date, LocalTime start, LocalTime end) {
        List<Seat> allSeats = seatDAO.findByFloorId(floorId);
        List<Booking> floorBookings = bookingDAO.findByDateAndFloor(date, floorId);

        Set<Integer> conflictingSeatIds = floorBookings.stream()
                .filter(b -> b.isOverlappingWith(date, start, end))
                .map(Booking::getSeatId)
                .collect(Collectors.toSet());

        return allSeats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
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
}
