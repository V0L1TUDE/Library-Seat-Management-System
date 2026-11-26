package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of SeatDAO querying the floor-wise seats table.
 */
public class SeatDAOImpl implements SeatDAO {

    @Override
    public Optional<Seat> findById(int id) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().seats.get(id));
        }

        String sql = "SELECT s.*, f.floor_name, f.floor_number " +
                     "FROM seats s " +
                     "JOIN floors f ON s.floor_id = f.floor_id " +
                     "WHERE s.seat_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.warning("MySQL query failed in SeatDAOImpl.findById: " + e.getMessage());
            return Optional.ofNullable(InMemoryDataStore.getInstance().seats.get(id));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Seat> findByFloorAndNumber(int floorId, String seatNumber) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getFloorId() == floorId && s.getSeatNumber().equalsIgnoreCase(seatNumber))
                    .findFirst();
        }

        String sql = "SELECT s.*, f.floor_name, f.floor_number " +
                     "FROM seats s " +
                     "JOIN floors f ON s.floor_id = f.floor_id " +
                     "WHERE s.floor_id = ? AND s.seat_number = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorId);
            ps.setString(2, seatNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getFloorId() == floorId && s.getSeatNumber().equalsIgnoreCase(seatNumber))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Seat> findByZoneAndNumber(int zoneId, String seatNumber) {
        return findByFloorAndNumber(zoneId, seatNumber);
    }

    @Override
    public List<Seat> findByFloorId(int floorId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getFloorId() == floorId)
                    .sorted(Comparator.comparing(Seat::getSeatNumber))
                    .toList();
        }

        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT s.*, f.floor_name, f.floor_number " +
                     "FROM seats s " +
                     "JOIN floors f ON s.floor_id = f.floor_id " +
                     "WHERE s.floor_id = ? " +
                     "ORDER BY s.seat_number ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.warning("MySQL query failed in SeatDAOImpl.findByFloorId: " + e.getMessage());
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .filter(s -> s.getFloorId() == floorId)
                    .sorted(Comparator.comparing(Seat::getSeatNumber))
                    .toList();
        }
        return seats;
    }

    @Override
    public List<Seat> findByZoneId(int zoneId) {
        return findByFloorId(zoneId);
    }

    @Override
    public List<Seat> findAll() {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .sorted(Comparator.comparingInt(Seat::getFloorNumber).thenComparing(Seat::getSeatNumber))
                    .toList();
        }

        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT s.*, f.floor_name, f.floor_number " +
                     "FROM seats s " +
                     "JOIN floors f ON s.floor_id = f.floor_id " +
                     "ORDER BY f.floor_number ASC, s.seat_number ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                seats.add(mapResultSetToSeat(rs));
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().seats.values().stream()
                    .sorted(Comparator.comparingInt(Seat::getFloorNumber).thenComparing(Seat::getSeatNumber))
                    .toList();
        }
        return seats;
    }

    @Override
    public boolean save(Seat seat) {
        if (!DBConnection.getInstance().isAvailable()) {
            if (seat.getId() == 0) seat.setId(InMemoryDataStore.getInstance().nextSeatId());
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }

        String sql = "INSERT INTO seats (floor_id, seat_number, seat_status, has_power, is_near_window, has_pc) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, seat.getFloorId());
            ps.setString(2, seat.getSeatNumber());
            ps.setString(3, seat.getStatus().name());
            ps.setBoolean(4, seat.isHasPower());
            ps.setBoolean(5, seat.isNearWindow());
            ps.setBoolean(6, seat.isHasPc());

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
            LoggerUtil.warning("Failed to save seat in MySQL: " + e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public boolean update(Seat seat) {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore.getInstance().seats.put(seat.getId(), seat);
            return true;
        }

        String sql = "UPDATE seats SET floor_id = ?, seat_number = ?, seat_status = ?, " +
                     "has_power = ?, is_near_window = ?, has_pc = ? WHERE seat_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, seat.getFloorId());
            ps.setString(2, seat.getSeatNumber());
            ps.setString(3, seat.getStatus().name());
            ps.setBoolean(4, seat.isHasPower());
            ps.setBoolean(5, seat.isNearWindow());
            ps.setBoolean(6, seat.isHasPc());
            ps.setInt(7, seat.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to update seat in MySQL: " + e.getMessage());
            return false;
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

        String sql = "UPDATE seats SET seat_status = ? WHERE seat_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, seatId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to update seat status in MySQL: " + e.getMessage());
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

        String sql = "SELECT COUNT(*) FROM seats WHERE seat_status = ?";
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
                rs.getInt("seat_id"),
                rs.getInt("floor_id"),
                rs.getString("seat_number"),
                rs.getBoolean("has_power"),
                rs.getBoolean("is_near_window"),
                rs.getBoolean("has_pc"),
                SeatStatus.valueOf(rs.getString("seat_status")),
                ts != null ? ts.toLocalDateTime() : null
        );
        seat.setFloorName(rs.getString("floor_name"));
        seat.setFloorNumber(rs.getInt("floor_number"));
        return seat;
    }
}
