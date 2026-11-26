package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.FloorDAO;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of FloorDAO managing library floor sections
 * and ensuring 20 seats exist per floor.
 */
public class FloorDAOImpl implements FloorDAO {

    @Override
    public Optional<Floor> findById(int floorId) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().floors.get(floorId));
        }

        String sql = "SELECT * FROM floors WHERE floor_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFloor(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.warning("MySQL query failed in FloorDAOImpl.findById: " + e.getMessage());
            return Optional.ofNullable(InMemoryDataStore.getInstance().floors.get(floorId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Floor> findByFloorNumber(int floorNumber) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> f.getFloorNumber() == floorNumber)
                    .findFirst();
        }

        String sql = "SELECT * FROM floors WHERE floor_number = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFloor(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> f.getFloorNumber() == floorNumber)
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Floor> findByFloorName(String floorName) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> f.getFloorName().equalsIgnoreCase(floorName))
                    .findFirst();
        }

        String sql = "SELECT * FROM floors WHERE LOWER(floor_name) = LOWER(?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, floorName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFloor(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> f.getFloorName().equalsIgnoreCase(floorName))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public List<Floor> findAll(boolean activeOnly) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> !activeOnly || f.isActive())
                    .sorted(Comparator.comparingInt(Floor::getFloorNumber))
                    .toList();
        }

        List<Floor> list = new ArrayList<>();
        String sql = activeOnly ? "SELECT * FROM floors WHERE is_active = TRUE ORDER BY floor_number ASC"
                                : "SELECT * FROM floors ORDER BY floor_number ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToFloor(rs));
            }
        } catch (SQLException e) {
            LoggerUtil.warning("MySQL query failed in FloorDAOImpl.findAll: " + e.getMessage());
            return InMemoryDataStore.getInstance().floors.values().stream()
                    .filter(f -> !activeOnly || f.isActive())
                    .sorted(Comparator.comparingInt(Floor::getFloorNumber))
                    .toList();
        }
        return list;
    }

    @Override
    public boolean save(Floor floor) {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore store = InMemoryDataStore.getInstance();
            if (floor.getFloorId() == 0) {
                floor.setFloorId(store.nextFloorId());
            }
            store.floors.put(floor.getFloorId(), floor);
            store.generateSeatsForFloor(floor, floor.getFloorNumber() == 2);
            return true;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            String insertFloorSql = "INSERT INTO floors (floor_name, floor_number, description, is_active) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertFloorSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, floor.getFloorName());
                ps.setInt(2, floor.getFloorNumber());
                ps.setString(3, floor.getDescription());
                ps.setBoolean(4, floor.isActive());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) {
                        floor.setFloorId(gk.getInt(1));
                    }
                }
            }

            // Verify whether MySQL trigger already populated 20 seats; if < 20, backfill safely with INSERT IGNORE
            int existingCount = 0;
            String countSql = "SELECT COUNT(*) FROM seats WHERE floor_id = ?";
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                countPs.setInt(1, floor.getFloorId());
                try (ResultSet rs = countPs.executeQuery()) {
                    if (rs.next()) {
                        existingCount = rs.getInt(1);
                    }
                }
            }

            if (existingCount < Floor.SEATS_PER_FLOOR) {
                String insertSeatSql = "INSERT IGNORE INTO seats (floor_id, seat_number, seat_status, has_power, is_near_window, has_pc) " +
                                       "VALUES (?, ?, 'AVAILABLE', ?, ?, ?)";
                try (PreparedStatement seatPs = conn.prepareStatement(insertSeatSql)) {
                    for (int i = 1; i <= Floor.SEATS_PER_FLOOR; i++) {
                        String seatNum = String.format("S-%02d", i);
                        boolean hasPower = i <= 16;
                        boolean nearWindow = (i == 1 || i == 5 || i == 6 || i == 10 || i == 11 || i == 15 || i == 16 || i == 20);
                        boolean hasPc = (floor.getFloorNumber() == 2) || i <= 4;

                        seatPs.setInt(1, floor.getFloorId());
                        seatPs.setString(2, seatNum);
                        seatPs.setBoolean(3, hasPower);
                        seatPs.setBoolean(4, nearWindow);
                        seatPs.setBoolean(5, hasPc);
                        seatPs.addBatch();
                    }
                    seatPs.executeBatch();
                }
            }

            conn.commit();
            LoggerUtil.info("Created floor '" + floor.getFloorName() + "' (ID=" + floor.getFloorId() + ") with 20 seats.");
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            LoggerUtil.severe("Failed to save floor in MySQL: " + e.getMessage(), e);
            return false;
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
    public boolean update(Floor floor) {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore.getInstance().floors.put(floor.getFloorId(), floor);
            return true;
        }

        String sql = "UPDATE floors SET floor_name = ?, floor_number = ?, description = ?, is_active = ? WHERE floor_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, floor.getFloorName());
            ps.setInt(2, floor.getFloorNumber());
            ps.setString(3, floor.getDescription());
            ps.setBoolean(4, floor.isActive());
            ps.setInt(5, floor.getFloorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to update floor: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int countTotalFloors() {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().floors.size();
        }

        String sql = "SELECT COUNT(*) FROM floors WHERE is_active = TRUE";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().floors.size();
        }
        return 0;
    }

    @Override
    public List<Floor> getFloorOccupancySummary(LocalDate date, LocalTime start, LocalTime end) {
        if (!DBConnection.getInstance().isAvailable()) {
            return computeInMemorySummary(date, start, end);
        }

        List<Floor> floors = new ArrayList<>();
        String sql = "SELECT f.*, " +
                "COUNT(DISTINCT s.seat_id) AS total_seats, " +
                "COUNT(DISTINCT CASE WHEN b_occ.seat_id IS NOT NULL THEN s.seat_id END) AS occupied_seats, " +
                "COUNT(DISTINCT CASE WHEN b_occ.seat_id IS NULL AND b_res.seat_id IS NOT NULL THEN s.seat_id END) AS reserved_seats " +
                "FROM floors f " +
                "LEFT JOIN seats s ON f.floor_id = s.floor_id " +
                "LEFT JOIN bookings b_occ ON s.seat_id = b_occ.seat_id AND b_occ.booking_date = ? AND b_occ.booking_status = 'OCCUPIED' " +
                "AND b_occ.start_time < ? AND b_occ.end_time > ? " +
                "LEFT JOIN bookings b_res ON s.seat_id = b_res.seat_id AND b_res.booking_date = ? AND b_res.booking_status = 'RESERVED' " +
                "AND b_res.start_time < ? AND b_res.end_time > ? " +
                "WHERE f.is_active = TRUE " +
                "GROUP BY f.floor_id " +
                "ORDER BY f.floor_number ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Date sqlDate = Date.valueOf(date);
            Time sqlEnd = Time.valueOf(end);
            Time sqlStart = Time.valueOf(start);

            ps.setDate(1, sqlDate);
            ps.setTime(2, sqlEnd);
            ps.setTime(3, sqlStart);
            ps.setDate(4, sqlDate);
            ps.setTime(5, sqlEnd);
            ps.setTime(6, sqlStart);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Floor floor = mapResultSetToFloor(rs);
                    int total = rs.getInt("total_seats");
                    int occ = rs.getInt("occupied_seats");
                    int res = rs.getInt("reserved_seats");
                    int avail = Math.max(0, total - occ - res);

                    floor.setTotalSeats(total);
                    floor.setOccupiedSeats(occ);
                    floor.setReservedSeats(res);
                    floor.setAvailableSeats(avail);
                    floors.add(floor);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.warning("Failed to fetch MySQL floor summary: " + e.getMessage());
            return computeInMemorySummary(date, start, end);
        }
        return floors;
    }

    private List<Floor> computeInMemorySummary(LocalDate date, LocalTime start, LocalTime end) {
        InMemoryDataStore store = InMemoryDataStore.getInstance();
        List<Floor> list = new ArrayList<>();
        for (Floor f : findAll(true)) {
            List<Seat> floorSeats = store.seats.values().stream()
                    .filter(s -> s.getFloorId() == f.getFloorId())
                    .toList();
            int total = floorSeats.size();
            int occ = 0;
            int res = 0;
            for (Seat s : floorSeats) {
                List<Booking> overlaps = store.bookings.values().stream()
                        .filter(b -> b.getSeatId() == s.getId() && b.isOverlappingWith(date, start, end))
                        .toList();
                if (overlaps.stream().anyMatch(b -> b.getStatus().name().equals("OCCUPIED"))) occ++;
                else if (!overlaps.isEmpty()) res++;
            }
            Floor copy = new Floor(f.getFloorId(), f.getFloorName(), f.getFloorNumber(), f.getDescription(), f.isActive(), f.getCreatedAt());
            copy.setTotalSeats(total);
            copy.setOccupiedSeats(occ);
            copy.setReservedSeats(res);
            copy.setAvailableSeats(Math.max(0, total - occ - res));
            list.add(copy);
        }
        return list;
    }

    private Floor mapResultSetToFloor(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Floor(
                rs.getInt("floor_id"),
                rs.getString("floor_name"),
                rs.getInt("floor_number"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
