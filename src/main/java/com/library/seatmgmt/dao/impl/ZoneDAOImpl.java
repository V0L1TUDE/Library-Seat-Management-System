package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.ZoneDAO;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.ZoneType;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of ZoneDAO with seamless In-Memory offline fallback.
 */
public class ZoneDAOImpl implements ZoneDAO {

    @Override
    public Optional<Zone> findById(int id) {
        if (!DBConnection.getInstance().isAvailable()) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().zones.get(id));
        }

        String sql = "SELECT * FROM zones WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToZone(rs));
                }
            }
        } catch (SQLException e) {
            return Optional.ofNullable(InMemoryDataStore.getInstance().zones.get(id));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Zone> findByCode(String code) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().zones.values().stream()
                    .filter(z -> z.getCode().equalsIgnoreCase(code))
                    .findFirst();
        }

        String sql = "SELECT * FROM zones WHERE code = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToZone(rs));
                }
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().zones.values().stream()
                    .filter(z -> z.getCode().equalsIgnoreCase(code))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public List<Zone> findAll(boolean activeOnly) {
        if (!DBConnection.getInstance().isAvailable()) {
            return InMemoryDataStore.getInstance().zones.values().stream()
                    .filter(z -> !activeOnly || z.isActive())
                    .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                    .toList();
        }

        List<Zone> zones = new ArrayList<>();
        String sql = activeOnly ? "SELECT * FROM zones WHERE is_active = TRUE ORDER BY id ASC"
                                : "SELECT * FROM zones ORDER BY id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                zones.add(mapResultSetToZone(rs));
            }
        } catch (SQLException e) {
            return InMemoryDataStore.getInstance().zones.values().stream()
                    .filter(z -> !activeOnly || z.isActive())
                    .toList();
        }
        return zones;
    }

    @Override
    public boolean save(Zone zone) {
        if (!DBConnection.getInstance().isAvailable()) {
            if (zone.getId() == 0) zone.setId(InMemoryDataStore.getInstance().zones.size() + 1);
            InMemoryDataStore.getInstance().zones.put(zone.getId(), zone);
            return true;
        }

        String sql = "INSERT INTO zones (name, code, description, zone_type, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, zone.getName());
            ps.setString(2, zone.getCode());
            ps.setString(3, zone.getDescription());
            ps.setString(4, zone.getZoneType().name());
            ps.setBoolean(5, zone.isActive());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        zone.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            InMemoryDataStore.getInstance().zones.put(zone.getId(), zone);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(Zone zone) {
        if (!DBConnection.getInstance().isAvailable()) {
            InMemoryDataStore.getInstance().zones.put(zone.getId(), zone);
            return true;
        }

        String sql = "UPDATE zones SET name = ?, code = ?, description = ?, zone_type = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, zone.getName());
            ps.setString(2, zone.getCode());
            ps.setString(3, zone.getDescription());
            ps.setString(4, zone.getZoneType().name());
            ps.setBoolean(5, zone.isActive());
            ps.setInt(6, zone.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            InMemoryDataStore.getInstance().zones.put(zone.getId(), zone);
            return true;
        }
    }

    @Override
    public boolean toggleActive(int zoneId, boolean active) {
        if (!DBConnection.getInstance().isAvailable()) {
            Zone z = InMemoryDataStore.getInstance().zones.get(zoneId);
            if (z != null) {
                z.setActive(active);
                return true;
            }
            return false;
        }

        String sql = "UPDATE zones SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, zoneId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Zone z = InMemoryDataStore.getInstance().zones.get(zoneId);
            if (z != null) {
                z.setActive(active);
                return true;
            }
            return false;
        }
    }

    @Override
    public int countTotalZones() {
        if (!DBConnection.getInstance().isAvailable()) {
            return (int) InMemoryDataStore.getInstance().zones.values().stream().filter(Zone::isActive).count();
        }

        String sql = "SELECT COUNT(*) FROM zones WHERE is_active = TRUE";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return (int) InMemoryDataStore.getInstance().zones.values().stream().filter(Zone::isActive).count();
        }
        return 0;
    }

    private Zone mapResultSetToZone(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Zone(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("description"),
                ZoneType.valueOf(rs.getString("zone_type")),
                rs.getBoolean("is_active"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
