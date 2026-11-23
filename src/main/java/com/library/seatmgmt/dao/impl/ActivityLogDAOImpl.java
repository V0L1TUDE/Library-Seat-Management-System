package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.exception.DatabaseOperationException;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation of ActivityLogDAO.
 */
public class ActivityLogDAOImpl implements ActivityLogDAO {

    @Override
    public boolean save(ActivityLog log) {
        String sql = "INSERT INTO activity_logs (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (log.getUserId() != null) {
                ps.setInt(1, log.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDetails());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        log.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error saving activity log: " + log.getAction(), e);
            throw new DatabaseOperationException("Failed to save activity log", e);
        }
        return false;
    }

    @Override
    public List<ActivityLog> findRecentLogs(int limit) {
        List<ActivityLog> list = new ArrayList<>();
        String sql = "SELECT l.*, u.username " +
                     "FROM activity_logs l " +
                     "LEFT JOIN users u ON l.user_id = u.id " +
                     "ORDER BY l.log_time DESC LIMIT ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("log_time");
                    ActivityLog log = new ActivityLog(
                            rs.getInt("id"),
                            rs.getObject("user_id") != null ? rs.getInt("user_id") : null,
                            rs.getString("action"),
                            rs.getString("details"),
                            ts != null ? ts.toLocalDateTime() : null
                    );
                    log.setUsername(rs.getString("username"));
                    list.add(log);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding recent activity logs", e);
        }
        return list;
    }
}
