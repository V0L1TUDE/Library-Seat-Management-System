package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.UserDAO;
import com.library.seatmgmt.exception.DatabaseOperationException;
import com.library.seatmgmt.model.Admin;
import com.library.seatmgmt.model.Student;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.enums.Role;
import com.library.seatmgmt.util.DBConnection;
import com.library.seatmgmt.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of UserDAO.
 * Demonstrates PreparedStatement, ResultSet mapping, Polymorphism, and Resource Management.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding user by ID: " + id, e);
            throw new DatabaseOperationException("Failed to find user by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding user by username: " + username, e);
            throw new DatabaseOperationException("Failed to find user by username", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding user by email: " + email, e);
            throw new DatabaseOperationException("Failed to find user by email", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Student> findStudentByRollNumber(String rollNumber) {
        String sql = "SELECT * FROM users WHERE student_id_number = ? AND role = 'STUDENT'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rollNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of((Student) mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error finding student by roll number: " + rollNumber, e);
            throw new DatabaseOperationException("Failed to find student by roll number", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean save(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role, student_id_number, department, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole().name());

            if (user instanceof Student s) {
                ps.setString(6, s.getStudentIdNumber());
                ps.setString(7, s.getDepartment());
                ps.setString(8, s.getPhone());
            } else if (user instanceof Admin a) {
                ps.setNull(6, Types.VARCHAR);
                ps.setString(7, a.getDepartment());
                ps.setNull(8, Types.VARCHAR);
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error saving user: " + user.getUsername(), e);
            throw new DatabaseOperationException("Failed to save user", e);
        }
        return false;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, department = ?, phone = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());

            if (user instanceof Student s) {
                ps.setString(3, s.getDepartment());
                ps.setString(4, s.getPhone());
            } else if (user instanceof Admin a) {
                ps.setString(3, a.getDepartment());
                ps.setNull(4, Types.VARCHAR);
            }

            ps.setInt(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.severe("Error updating user ID: " + user.getId(), e);
            throw new DatabaseOperationException("Failed to update user", e);
        }
    }

    @Override
    public List<Student> findAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' ORDER BY full_name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                students.add((Student) mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error fetching all students", e);
            throw new DatabaseOperationException("Failed to fetch students", e);
        }
        return students;
    }

    @Override
    public int countTotalStudents() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LoggerUtil.severe("Error counting total students", e);
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        Timestamp ts = rs.getTimestamp("created_at");

        if (role == Role.STUDENT) {
            return new Student(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    ts != null ? ts.toLocalDateTime() : null,
                    rs.getString("student_id_number"),
                    rs.getString("department"),
                    rs.getString("phone")
            );
        } else {
            return new Admin(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    ts != null ? ts.toLocalDateTime() : null,
                    rs.getString("department")
            );
        }
    }
}
