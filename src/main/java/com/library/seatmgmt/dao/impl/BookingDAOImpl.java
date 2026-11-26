package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * JDBC Implementation of BookingDAO with transaction control and In-Memory offline fallback.
 */
public class BookingDAOImpl implements BookingDAO {

    @Override
    public Optional<Booking> findById(int id) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().bookings.get(id));
        }

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
            return Optional.ofNullable(InMemoryDataStore.getInstance().bookings.get(id));
        }
        return Optional.empty();
    }

    @Override
    public List<Booking> findByUserId(int userId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getUserId() == userId)
                    .sorted((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()))
                    .toList();
        }

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
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getUserId() == userId)
                    .toList();
        }
        return list;
    }

    @Override
    public List<Booking> findActiveByUserId(int userId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getUserId() == userId && b.getStatus().isActive())
                    .sorted(Comparator.comparing(Booking::getBookingDate).thenComparing(Booking::getStartTime))
                    .toList();
        }

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
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getUserId() == userId && b.getStatus().isActive())
                    .toList();
        }
        return list;
    }

    @Override
    public List<Booking> findByDateAndZone(LocalDate date, int zoneId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> Objects.equals(b.getBookingDate(), date) && b.getStatus().isActive())
                    .filter(b -> {
                        Seat s = InMemoryDataStore.getInstance().seats.get(b.getSeatId());
                        return s != null && s.getZoneId() == zoneId;
                    })
                    .toList();
        }

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
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> Objects.equals(b.getBookingDate(), date) && b.getStatus().isActive())
                    .filter(b -> {
                        Seat s = InMemoryDataStore.getInstance().seats.get(b.getSeatId());
                        return s != null && s.getZoneId() == zoneId;
                    })
                    .toList();
        }
        return list;
    }

    @Override
    public List<Booking> findOverlappingBookings(int seatId, LocalDate date, LocalTime start, LocalTime end) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getSeatId() == seatId && b.isOverlappingWith(date, start, end))
                    .toList();
        }

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
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getSeatId() == seatId && b.isOverlappingWith(date, start, end))
                    .toList();
        }
        return list;
    }

    @Override
    public boolean saveTransactional(Booking booking) throws BookingConflictException {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore store = InMemoryDataStore.getInstance();
            synchronized (store) {
                // Check in-memory overlap atomically
                boolean seatConflict = store.bookings.values().stream()
                        .anyMatch(b -> b.getSeatId() == booking.getSeatId() &&
                                b.isOverlappingWith(booking.getBookingDate(), booking.getStartTime(), booking.getEndTime()));

                if (seatConflict) {
                    throw new BookingConflictException("The selected seat has already been reserved for this time slot.");
                }

                boolean userConflict = store.bookings.values().stream()
                        .anyMatch(b -> b.getUserId() == booking.getUserId() &&
                                b.isOverlappingWith(booking.getBookingDate(), booking.getStartTime(), booking.getEndTime()));

                if (userConflict) {
                    throw new BookingConflictException("You already hold an active reservation during this time slot.");
                }

                booking.setId(store.nextBookingId());

                Seat s = store.seats.get(booking.getSeatId());
                if (s != null) {
                    booking.setSeatNumber(s.getSeatNumber());
                    booking.setZoneName(s.getZoneName());
                }

                User u = store.users.get(booking.getUserId());
                if (u != null) {
                    booking.setStudentName(u.getFullName());
                    if (u instanceof com.library.seatmgmt.model.Student stu) {
                        booking.setStudentIdNumber(stu.getStudentIdNumber());
                    }
                }

                store.bookings.put(booking.getId(), booking);
                return true;
            }
        }

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

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
                    conn.commit();
                    return true;
                }
            }

            conn.rollback();
            return false;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new BookingConflictException("Database error during reservation: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public boolean updateStatus(int bookingId, BookingStatus status) {
        if (!DBConnection.getInstance().isAvailable()) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(status);
                return true;
            }
            return false;
        }

        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(status);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean checkIn(int bookingId, LocalDateTime checkInTime) {
        if (!DBConnection.getInstance().isAvailable()) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(BookingStatus.OCCUPIED);
                b.setCheckInTime(checkInTime);
                return true;
            }
            return false;
        }

        String sql = "UPDATE bookings SET status = 'OCCUPIED', check_in_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkInTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(BookingStatus.OCCUPIED);
                b.setCheckInTime(checkInTime);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean checkOut(int bookingId, LocalDateTime checkOutTime) {
        if (!DBConnection.getInstance().isAvailable()) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(BookingStatus.COMPLETED);
                b.setCheckOutTime(checkOutTime);
                return true;
            }
            return false;
        }

        String sql = "UPDATE bookings SET status = 'COMPLETED', check_out_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkOutTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Booking b = InMemoryDataStore.getInstance().bookings.get(bookingId);
            if (b != null) {
                b.setStatus(BookingStatus.COMPLETED);
                b.setCheckOutTime(checkOutTime);
                return true;
            }
            return false;
        }
    }

    @Override
    public List<Booking> findExpiredReservations(LocalDateTime cutoffTime) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.isNoShow(cutoffTime))
                    .toList();
        }

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

            LocalDateTime graceThreshold = cutoffTime.minusMinutes(15);
            ps.setDate(1, Date.valueOf(graceThreshold.toLocalDate()));
            ps.setDate(2, Date.valueOf(graceThreshold.toLocalDate()));
            ps.setTime(3, Time.valueOf(graceThreshold.toLocalTime()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.isNoShow(cutoffTime))
                    .toList();
        }
        return list;
    }

    @Override
    public List<Booking> findAllRecent(int limit) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                    .limit(limit)
                    .toList();
        }

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
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .limit(limit)
                    .toList();
        }
        return list;
    }

    @Override
    public int countByStatus(BookingStatus status) {
        if (!DBConnection.getInstance().isAvailable()) {
            return (int) InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getStatus() == status).count();
        }

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
            return (int) InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getStatus() == status).count();
        }
        return 0;
    }

    @Override
    public int countBookingsOnDate(LocalDate date) {
        if (!DBConnection.getInstance().isAvailable()) {
            return (int) InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> Objects.equals(b.getBookingDate(), date)).count();
        }

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
            return (int) InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> Objects.equals(b.getBookingDate(), date)).count();
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
