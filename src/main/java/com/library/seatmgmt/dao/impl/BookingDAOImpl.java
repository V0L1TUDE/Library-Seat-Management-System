package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * JDBC Implementation of BookingDAO with transaction control against MySQL
 * (floor-wise seats & bookings tables) and In-Memory offline fallback.
 */
public class BookingDAOImpl implements BookingDAO {

    private static final String BASE_SELECT =
            "SELECT b.*, u.full_name AS student_name, u.student_id_number, " +
            "s.seat_number, s.floor_id, f.floor_name, f.floor_number " +
            "FROM bookings b " +
            "JOIN users u ON b.student_id = u.id " +
            "JOIN seats s ON b.seat_id = s.seat_id " +
            "JOIN floors f ON s.floor_id = f.floor_id ";

    @Override
    public Optional<Booking> findById(int id) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().bookings.get(id));
        }

        String sql = BASE_SELECT + "WHERE b.booking_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.warning("MySQL query failed in BookingDAOImpl.findById: " + e.getMessage());
            return Optional.ofNullable(InMemoryDataStore.getInstance().bookings.get(id));
        }
        return Optional.empty();
    }

    @Override
    public List<Booking> findByUserId(int userId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getUserId() == userId)
                    .sorted(Comparator.comparing(Booking::getBookingDate).reversed()
                            .thenComparing(Booking::getStartTime, Comparator.reverseOrder()))
                    .toList();
        }

        List<Booking> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE b.student_id = ? ORDER BY b.booking_date DESC, b.start_time DESC";
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
        String sql = BASE_SELECT + "WHERE b.student_id = ? AND b.booking_status IN ('RESERVED', 'OCCUPIED') " +
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
    public List<Booking> findByDateAndFloor(LocalDate date, int floorId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> Objects.equals(b.getBookingDate(), date) && b.getStatus().isActive())
                    .filter(b -> {
                        Seat s = InMemoryDataStore.getInstance().seats.get(b.getSeatId());
                        return s != null && s.getFloorId() == floorId;
                    })
                    .toList();
        }

        List<Booking> list = new ArrayList<>();
        String sql = BASE_SELECT +
                     "WHERE b.booking_date = ? AND s.floor_id = ? AND b.booking_status IN ('RESERVED', 'OCCUPIED') " +
                     "ORDER BY b.start_time ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, floorId);
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
                        return s != null && s.getFloorId() == floorId;
                    })
                    .toList();
        }
        return list;
    }

    @Override
    public List<Booking> findByDateAndZone(LocalDate date, int zoneId) {
        return findByDateAndFloor(date, zoneId);
    }

    @Override
    public List<Booking> findOverlappingBookings(int seatId, LocalDate date, LocalTime start, LocalTime end) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getSeatId() == seatId && b.isOverlappingWith(date, start, end))
                    .toList();
        }

        List<Booking> list = new ArrayList<>();
        String sql = BASE_SELECT +
                     "WHERE b.seat_id = ? AND b.booking_date = ? " +
                     "AND b.booking_status IN ('RESERVED', 'OCCUPIED') " +
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
                Seat s = store.seats.get(booking.getSeatId());
                if (s == null) {
                    throw new BookingConflictException("The selected seat does not exist.");
                }

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
                booking.setSeatNumber(s.getSeatNumber());
                booking.setFloorId(s.getFloorId());
                booking.setFloorName(s.getFloorName());
                booking.setFloorNumber(s.getFloorNumber());

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

            // 1. Lock seat row and load floor details
            String seatStatusSql = "SELECT s.seat_status, s.seat_number, s.floor_id, f.floor_name, f.floor_number " +
                                   "FROM seats s JOIN floors f ON s.floor_id = f.floor_id " +
                                   "WHERE s.seat_id = ? FOR UPDATE";
            try (PreparedStatement seatPs = conn.prepareStatement(seatStatusSql)) {
                seatPs.setInt(1, booking.getSeatId());
                try (ResultSet rs = seatPs.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new BookingConflictException("The selected seat does not exist.");
                    }
                    booking.setSeatNumber(rs.getString("seat_number"));
                    booking.setFloorId(rs.getInt("floor_id"));
                    booking.setFloorName(rs.getString("floor_name"));
                    booking.setFloorNumber(rs.getInt("floor_number"));
                }
            }

            // 2. Check overlapping booking on the same seat
            String seatConflictSql = "SELECT COUNT(*) FROM bookings " +
                    "WHERE seat_id = ? AND booking_date = ? " +
                    "AND booking_status IN ('RESERVED', 'OCCUPIED') " +
                    "AND start_time < ? AND end_time > ? FOR UPDATE";
            try (PreparedStatement checkPs = conn.prepareStatement(seatConflictSql)) {
                checkPs.setInt(1, booking.getSeatId());
                checkPs.setDate(2, Date.valueOf(booking.getBookingDate()));
                checkPs.setTime(3, Time.valueOf(booking.getEndTime()));
                checkPs.setTime(4, Time.valueOf(booking.getStartTime()));

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        throw new BookingConflictException("The selected seat has already been reserved or occupied for this time slot.");
                    }
                }
            }

            // 3. Check student overlap across all floors
            String userConflictSql = "SELECT COUNT(*) FROM bookings " +
                    "WHERE student_id = ? AND booking_date = ? " +
                    "AND booking_status IN ('RESERVED', 'OCCUPIED') " +
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

            // 4. Insert booking record
            String insertSql = "INSERT INTO bookings (student_id, seat_id, booking_date, start_time, end_time, booking_status) " +
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

        String sql = "UPDATE bookings SET booking_status = ? WHERE booking_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to update booking status in MySQL: " + e.getMessage());
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

        String sql = "UPDATE bookings SET booking_status = 'OCCUPIED', check_in_time = ? WHERE booking_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkInTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to check in booking in MySQL: " + e.getMessage());
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

        String sql = "UPDATE bookings SET booking_status = 'COMPLETED', check_out_time = ? WHERE booking_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(checkOutTime));
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to check out booking in MySQL: " + e.getMessage());
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
        String sql = BASE_SELECT +
                     "WHERE b.booking_status = 'RESERVED' " +
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
        String sql = BASE_SELECT + "ORDER BY b.created_at DESC, b.booking_id DESC LIMIT ?";
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
    public List<Booking> findAllRecentByFloor(int floorId, int limit) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getFloorId() == floorId)
                    .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                    .limit(limit)
                    .toList();
        }

        List<Booking> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE s.floor_id = ? ORDER BY b.created_at DESC, b.booking_id DESC LIMIT ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().bookings.values().stream()
                    .filter(b -> b.getFloorId() == floorId)
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

        String sql = "SELECT COUNT(*) FROM bookings WHERE booking_status = ?";
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
                rs.getInt("booking_id"),
                rs.getInt("student_id"),
                rs.getInt("seat_id"),
                rs.getDate("booking_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                BookingStatus.valueOf(rs.getString("booking_status")),
                checkInTs != null ? checkInTs.toLocalDateTime() : null,
                checkOutTs != null ? checkOutTs.toLocalDateTime() : null,
                createdTs != null ? createdTs.toLocalDateTime() : null,
                updatedTs != null ? updatedTs.toLocalDateTime() : null
        );

        b.setStudentName(rs.getString("student_name"));
        b.setStudentIdNumber(rs.getString("student_id_number"));
        b.setSeatNumber(rs.getString("seat_number"));
        b.setFloorId(rs.getInt("floor_id"));
        b.setFloorName(rs.getString("floor_name"));
        b.setFloorNumber(rs.getInt("floor_number"));
        return b;
    }
}
