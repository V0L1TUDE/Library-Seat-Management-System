package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.ZoneDAO;
import com.library.seatmgmt.exception.DatabaseOperationException;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.ZoneType;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of ZoneDAO.
 */
public class ZoneDAOImpl implements ZoneDAO {

    @Override
    public Optional<Zone> findById(int id) {
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
            LoggerUtil.severe("Error finding zone by ID: " + id, e);
            throw new DatabaseOperationException("Failed to find zone by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Zone> findByCode(String code) {
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
            LoggerUtil.severe("Error finding zone by code: " + code, e);
            throw new DatabaseOperationException("Failed to find zone by code", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Zone> findAll(boolean activeOnly) {
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
            LoggerUtil.severe("Error finding all zones", e);
            throw new DatabaseOperationException("Failed to fetch zones", e);
        }
        return zones;
    }

    @Override
    public boolean save(Zone zone) {
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
            LoggerUtil.severe("Error saving zone: " + zone.getName(), e);
            throw new DatabaseOperationException("Failed to save zone", e);
        }
        return false;
    }

    @Override
    public boolean update(Zone zone) {
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
            LoggerUtil.severe("Error updating zone ID: " + zone.getId(), e);
            throw new DatabaseOperationException("Failed to update zone", e);
        }
    }

    @Override
    public boolean toggleActive(int zoneId, boolean active) {
        String sql = "UPDATE zones SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, zoneId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.severe("Error toggling zone status for ID: " + zoneId, e);
            throw new DatabaseOperationException("Failed to update zone status", e);
        }
    }

    @Override
    public int countTotalZones() {
        String sql = "SELECT COUNT(*) FROM zones WHERE is_active = TRUE";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error counting zones", e);
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
