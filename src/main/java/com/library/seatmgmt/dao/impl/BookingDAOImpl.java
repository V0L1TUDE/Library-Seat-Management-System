package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.exception.DatabaseOperationException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of BookingDAO.
 * Demonstrates JDBC Transaction Handling with setAutoCommit(false), commit(), and rollback().
 */
public class BookingDAOImpl implements BookingDAO {

    @Override
    public Optional<Booking> findById(int id) {
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding booking by ID: " + id, e);
            throw new DatabaseOperationException("Failed to find booking", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Booking> findByUserId(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.user_id = ? " +
                     "ORDER BY b.booking_date DESC, b.start_time DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding bookings for user: " + userId, e);
            throw new DatabaseOperationException("Failed to fetch user bookings", e);
        }
        return list;
    }

    @Override
    public List<Booking> findActiveByUserId(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.user_id = ? AND b.status IN ('RESERVED', 'OCCUPIED') " +
                     "ORDER BY b.booking_date ASC, b.start_time ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding active bookings for user: " + userId, e);
            throw new DatabaseOperationException("Failed to fetch active bookings", e);
        }
        return list;
    }

    @Override
    public List<Booking> findByDateAndZone(LocalDate date, int zoneId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.booking_date = ? AND s.zone_id = ? AND b.status IN ('RESERVED', 'OCCUPIED') " +
                     "ORDER BY b.start_time ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding bookings for date and zone", e);
            throw new DatabaseOperationException("Failed to fetch zone bookings", e);
        }
        return list;
    }

    @Override
    public List<Booking> findOverlappingBookings(int seatId, LocalDate date, LocalTime start, LocalTime end) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.seat_id = ? AND b.booking_date = ? " +
                     "AND b.status IN ('RESERVED', 'OCCUPIED') " +
                     "AND b.start_time < ? AND b.end_time > ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, seatId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(end));
            ps.setTime(4, Time.valueOf(start));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding overlapping bookings", e);
            throw new DatabaseOperationException("Failed to query booking conflicts", e);
        }
        return list;
    }

    /**
     * Atomically validates conflict rules and creates a reservation using a JDBC Transaction.
     * Demonstrates conn.setAutoCommit(false), conn.commit(), and conn.rollback().
     */
    @Override
    public boolean saveTransactional(Booking booking) throws BookingConflictException {
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Check for seat overlap (Locking via FOR UPDATE)
            String seatConflictSql = "SELECT COUNT(*) FROM bookings " +
                    "WHERE seat_id = ? AND booking_date = ? " +
                    "AND status IN ('RESERVED', 'OCCUPIED') " +
                    "AND start_time < ? AND end_time > ? FOR UPDATE";

            try (PreparedStatement checkPs = conn.prepareStatement(seatConflictSql)) {
                checkPs.setInt(1, booking.getSeatId());
                checkPs.setDate(2, Date.valueOf(booking.getBookingDate()));
                checkPs.setTime(3, Time.valueOf(booking.getEndTime()));
                checkPs.setTime(4, Time.valueOf(booking.getStartTime()));

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        throw new BookingConflictException("The selected seat has already been reserved for this time slot.");
                    }
                }
            }

            // 2. Check if student already has another active booking during this time
            String userConflictSql = "SELECT COUNT(*) FROM bookings " +
                    "WHERE user_id = ? AND booking_date = ? " +
                    "AND status IN ('RESERVED', 'OCCUPIED') " +
                    "AND start_time < ? AND end_time > ?";

            try (PreparedStatement userPs = conn.prepareStatement(userConflictSql)) {
                userPs.setInt(1, booking.getUserId());
                userPs.setDate(2, Date.valueOf(booking.getBookingDate()));
                userPs.setTime(3, Time.valueOf(booking.getEndTime()));
                userPs.setTime(4, Time.valueOf(booking.getStartTime()));

                try (ResultSet rs = userPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        throw new BookingConflictException("You already hold an active reservation during this time slot.");
                    }
                }
            }

            // 3. Insert the new booking
            String insertSql = "INSERT INTO bookings (user_id, seat_id, booking_date, start_time, end_time, status) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setInt(1, booking.getUserId());
                insertPs.setInt(2, booking.getSeatId());
                insertPs.setDate(3, Date.valueOf(booking.getBookingDate()));
                insertPs.setTime(4, Time.valueOf(booking.getStartTime()));
                insertPs.setTime(5, Time.valueOf(booking.getEndTime()));
                insertPs.setString(6, booking.getStatus().name());

                int affected = insertPs.executeUpdate();
                if (affected > 0) {
                    try (ResultSet gk = insertPs.getGeneratedKeys()) {
                        if (gk.next()) {
                            booking.setId(gk.getInt(1));
                        }
                    }
                    conn.commit(); // Commit Transaction
                    LoggerUtil.info("Booking #" + booking.getId() + " created successfully via transaction.");
                    return true;
                }
            }

            conn.rollback();
            return false;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LoggerUtil.severe("Rollback failed", ex);
                }
            }
            LoggerUtil.severe("Transaction failed for booking creation", e);
            throw new DatabaseOperationException("Failed to save booking in transaction", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LoggerUtil.severe("Error resetting auto-commit or closing connection", e);
                }
            }
        }
    }

    @Override
    public boolean updateStatus(int bookingId, BookingStatus status) {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.severe("Error updating status for booking ID: " + bookingId, e);
            throw new DatabaseOperationException("Failed to update booking status", e);
        }
    }

    @Override
    public boolean checkIn(int bookingId, LocalDateTime checkInTime) {
        String sql = "UPDATE bookings SET status = 'OCCUPIED', check_in_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkInTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.severe("Error checking in booking ID: " + bookingId, e);
            throw new DatabaseOperationException("Failed to check in", e);
        }
    }

    @Override
    public boolean checkOut(int bookingId, LocalDateTime checkOutTime) {
        String sql = "UPDATE bookings SET status = 'COMPLETED', check_out_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkOutTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.severe("Error checking out booking ID: " + bookingId, e);
            throw new DatabaseOperationException("Failed to check out", e);
        }
    }

    @Override
    public List<Booking> findExpiredReservations(LocalDateTime cutoffTime) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE b.status = 'RESERVED' " +
                     "AND (b.booking_date < ? OR (b.booking_date = ? AND b.start_time <= ?))";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(cutoffTime.toLocalDate()));
            ps.setDate(2, Date.valueOf(cutoffTime.toLocalDate()));
            ps.setTime(3, Time.valueOf(cutoffTime.toLocalTime().minusMinutes(15))); // 15-minute grace period

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding expired reservations", e);
        }
        return list;
    }

    @Override
    public List<Booking> findAllRecent(int limit) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
                     "s.seat_number, z.name AS zone_name " +
                     "FROM bookings b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "JOIN seats s ON b.seat_id = s.id " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "ORDER BY b.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding recent bookings", e);
        }
        return list;
    }

    @Override
    public int countByStatus(BookingStatus status) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE status = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error counting bookings by status", e);
        }
        return 0;
    }

    @Override
    public int countBookingsOnDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE booking_date = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error counting bookings on date", e);
        }
        return 0;
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Timestamp checkInTs = rs.getTimestamp("check_in_time");
        Timestamp checkOutTs = rs.getTimestamp("check_out_time");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        Booking b = new Booking(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("seat_id"),
                rs.getDate("booking_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                BookingStatus.valueOf(rs.getString("status")),
                checkInTs != null ? checkInTs.toLocalDateTime() : null,
                checkOutTs != null ? checkOutTs.toLocalDateTime() : null,
                createdTs != null ? createdTs.toLocalDateTime() : null,
                updatedTs != null ? updatedTs.toLocalDateTime() : null
        );

        b.setStudentName(rs.getString("student_name"));
        b.setStudentIdNumber(rs.getString("student_id_number"));
        b.setSeatNumber(rs.getString("seat_number"));
        b.setZoneName(rs.getString("zone_name"));
        return b;
    }
}
