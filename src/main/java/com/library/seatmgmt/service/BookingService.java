package com.library.seatmgmt.service;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.exception.SeatUnavailableException;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing reservation placement, check-ins, check-outs, and cancellations.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final SeatDAO seatDAO;
    private final ActivityLogDAO logDAO;

    public BookingService() {
        this(new BookingDAOImpl(), new SeatDAOImpl(), new ActivityLogDAOImpl());
    }

    public BookingService(BookingDAO bookingDAO, SeatDAO seatDAO, ActivityLogDAO logDAO) {
        this.bookingDAO = bookingDAO;
        this.seatDAO = seatDAO;
        this.logDAO = logDAO;
    }

    /**
     * Validates and places a new seat booking via database transaction.
     */
    public Booking createBooking(int userId, int seatId, LocalDate date, LocalTime start, LocalTime end)
            throws InvalidBookingException, SeatUnavailableException, BookingConflictException {

        // 1. Validate parameters
        ValidationUtil.validateBookingSlot(date, start, end);

        // 2. Validate seat existence and operational status
        Optional<Seat> seatOpt = seatDAO.findById(seatId);
        if (seatOpt.isEmpty()) {
            throw new SeatUnavailableException("The specified seat does not exist.");
        }

        Seat seat = seatOpt.get();
        if (seat.getStatus() == SeatStatus.MAINTENANCE) {
            throw new SeatUnavailableException("Seat " + seat.getSeatNumber() + " is currently under maintenance.");
        }

        // 3. Atomically create booking via transaction
        Booking booking = new Booking(userId, seatId, date, start, end);
        boolean saved = bookingDAO.saveTransactional(booking);

        if (!saved) {
            throw new BookingConflictException("Unable to complete reservation due to conflict.");
        }

        // 4. Log event
        String floorLabel = seat.getFloorName() != null ? seat.getFloorName() + ", " : "";
        logDAO.save(new ActivityLog(userId, "BOOKING_CREATED",
                "Created booking #" + booking.getId() + " for " + floorLabel + "Seat " + seat.getSeatNumber() + " on " + date));

        return booking;
    }

    /**
     * Checks in a student to their reserved seat.
     */
    public boolean checkIn(int bookingId, int userId) throws InvalidBookingException {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) {
            throw new InvalidBookingException("Booking record not found.");
        }

        Booking booking = opt.get();
        if (booking.getUserId() != userId) {
            throw new InvalidBookingException("You are not authorized to check into this booking.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!booking.isEligibleForCheckIn(now)) {
            throw new InvalidBookingException("Check-in is only allowed between 15 minutes before and 15 minutes after the slot start time.");
        }

        boolean success = bookingDAO.checkIn(bookingId, now);
        if (success) {
            logDAO.save(new ActivityLog(userId, "CHECK_IN", "Checked in to booking #" + bookingId));
            LoggerUtil.info("Booking #" + bookingId + " checked in successfully.");
        }
        return success;
    }

    /**
     * Checks out a student and releases the seat.
     */
    public boolean checkOut(int bookingId, int userId) throws InvalidBookingException {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) {
            throw new InvalidBookingException("Booking record not found.");
        }

        Booking booking = opt.get();
        if (booking.getUserId() != userId) {
            throw new InvalidBookingException("You are not authorized to check out of this booking.");
        }

        if (booking.getStatus() != BookingStatus.OCCUPIED) {
            throw new InvalidBookingException("Only currently occupied bookings can be checked out.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean success = bookingDAO.checkOut(bookingId, now);
        if (success) {
            logDAO.save(new ActivityLog(userId, "CHECK_OUT", "Checked out of booking #" + bookingId));
            LoggerUtil.info("Booking #" + bookingId + " checked out successfully.");
        }
        return success;
    }

    /**
     * Cancels an upcoming booking.
     */
    public boolean cancelBooking(int bookingId, int userId) throws InvalidBookingException {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) {
            throw new InvalidBookingException("Booking record not found.");
        }

        Booking booking = opt.get();
        if (booking.getUserId() != userId) {
            throw new InvalidBookingException("You are not authorized to cancel this booking.");
        }

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new InvalidBookingException("Only active pending reservations can be cancelled.");
        }

        boolean success = bookingDAO.updateStatus(bookingId, BookingStatus.CANCELLED);
        if (success) {
            logDAO.save(new ActivityLog(userId, "BOOKING_CANCELLED", "Cancelled booking #" + bookingId));
            LoggerUtil.info("Booking #" + bookingId + " cancelled by student.");
        }
        return success;
    }

    public List<Booking> getUserBookings(int userId) {
        return bookingDAO.findByUserId(userId);
    }

    public List<Booking> getUserActiveBookings(int userId) {
        return bookingDAO.findActiveByUserId(userId);
    }
}
