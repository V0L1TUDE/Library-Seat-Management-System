package com.library.seatmgmt.dao;

import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Booking lifecycle and reservation queries.
 */
public interface BookingDAO {
    Optional<Booking> findById(int id);
    List<Booking> findByUserId(int userId);
    List<Booking> findActiveByUserId(int userId);
    List<Booking> findByDateAndFloor(LocalDate date, int floorId);
    List<Booking> findByDateAndZone(LocalDate date, int zoneId);
    List<Booking> findOverlappingBookings(int seatId, LocalDate date, LocalTime start, LocalTime end);
    boolean saveTransactional(Booking booking) throws BookingConflictException;
    boolean updateStatus(int bookingId, BookingStatus status);
    boolean checkIn(int bookingId, LocalDateTime checkInTime);
    boolean checkOut(int bookingId, LocalDateTime checkOutTime);
    List<Booking> findExpiredReservations(LocalDateTime cutoffTime);
    List<Booking> findAllRecent(int limit);
    List<Booking> findAllRecentByFloor(int floorId, int limit);
    int countByStatus(BookingStatus status);
    int countBookingsOnDate(LocalDate date);
}
