package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of SeatDAO with seamless In-Memory offline fallback.
 */
public class SeatDAOImpl implements SeatDAO {

    @Override
    public Optional<Seat> findById(int id) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().seats.get(id));
        }

        String sql = "SELECT s.*, z.name AS zone_name, z.code AS zone_code " +
                     "FROM seats s " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE s.id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().seats.get(id));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Seat> findByZoneAndNumber(int zoneId, String seatNumber) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getZoneId() == zoneId && s.getSeatNumber().equalsIgnoreCase(seatNumber))
                    .findFirst();
        }

        String sql = "SELECT s.*, z.name AS zone_name, z.code AS zone_code " +
                     "FROM seats s " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE s.zone_id = ? AND s.seat_number = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, zoneId);
            ps.setString(2, seatNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getZoneId() == zoneId && s.getSeatNumber().equalsIgnoreCase(seatNumber))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public List<Seat> findByZoneId(int zoneId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getZoneId() == zoneId)
                    .sorted((a, b) -> a.getSeatNumber().compareTo(b.getSeatNumber()))
                    .toList();
        }

        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT s.*, z.name AS zone_name, z.code AS zone_code " +
                     "FROM seats s " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "WHERE s.zone_id = ? " +
                     "ORDER BY s.seat_number ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getZoneId() == zoneId)
                    .toList();
        }
        return seats;
    }

    @Override
    public List<Seat> findAll() {
        if (!DBConnection.getInstance().isAvailable()) {
            return new ArrayList<>(InMemoryDataStore.getInstance().seats.values());
        }

        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT s.*, z.name AS zone_name, z.code AS zone_code " +
                     "FROM seats s " +
                     "JOIN zones z ON s.zone_id = z.id " +
                     "ORDER BY z.name ASC, s.seat_number ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                seats.add(mapResultSetToSeat(rs));
            }
        } catch (SQLException e) {
            return new ArrayList<>(InMemoryDataStore.getInstance().seats.values());
        }
        return seats;
    }

    @Override
    public boolean save(Seat seat) {
        if (!DBConnection.getInstance().isAvailable()) {
            if (seat.getId() == 0) seat.setId(InMemoryDataStore.getInstance().seats.size() + 1);
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }

        String sql = "INSERT INTO seats (zone_id, seat_number, has_power, is_near_window, has_pc, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, seat.getZoneId());
            ps.setString(2, seat.getSeatNumber());
            ps.setBoolean(3, seat.isHasPower());
            ps.setBoolean(4, seat.isNearWindow());
            ps.setBoolean(5, seat.isHasPc());
            ps.setString(6, seat.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        seat.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(Seat seat) {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }

        String sql = "UPDATE seats SET zone_id = ?, seat_number = ?, has_power = ?, is_near_window = ?, " +
                     "has_pc = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, seat.getZoneId());
            ps.setString(2, seat.getSeatNumber());
            ps.setBoolean(3, seat.isHasPower());
            ps.setBoolean(4, seat.isNearWindow());
            ps.setBoolean(5, seat.isHasPc());
            ps.setString(6, seat.getStatus().name());
            ps.setInt(7, seat.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }
    }

    @Override
    public boolean updateStatus(int seatId, SeatStatus status) {
        if (!DBConnection.getInstance().isAvailable()) {
            Seat s = InMemoryDataStore.getInstance().seats.get(seatId);
            if (s != null) {
                s.setStatus(status);
                return true;
            }
            return false;
        }

        String sql = "UPDATE seats SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, seatId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Seat s = InMemoryDataStore.getInstance().seats.get(seatId);
            if (s != null) {
                s.setStatus(status);
                return true;
            }
            return false;
        }
    }

    @Override
    public int countTotalSeats() {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.size();
        }

        String sql = "SELECT COUNT(*) FROM seats";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().seats.size();
        }
        return 0;
    }

    @Override
    public int countSeatsByStatus(SeatStatus status) {
        if (!DBConnection.getInstance().isAvailable()) {
            return (int) InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getStatus() == status).count();
        }

        String sql = "SELECT COUNT(*) FROM seats WHERE status = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            return (int) InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getStatus() == status).count();
        }
        return 0;
    }

    private Seat mapResultSetToSeat(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        Seat seat = new Seat(
                rs.getInt("id"),
                rs.getInt("zone_id"),
                rs.getString("seat_number"),
                rs.getBoolean("has_power"),
                rs.getBoolean("is_near_window"),
                rs.getBoolean("has_pc"),
                SeatStatus.valueOf(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
        seat.setZoneName(rs.getString("zone_name"));
        seat.setZoneCode(rs.getString("zone_code"));
        return seat;
    }
}
