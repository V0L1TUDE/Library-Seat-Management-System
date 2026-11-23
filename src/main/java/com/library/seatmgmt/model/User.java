package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.Role;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base entity representing a user in the system.
 * Demonstrates Abstraction and Encapsulation.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    // Default constructor for serialization and reflection
    public User() {
    }

    // Parameterized constructor
    public User(int id, String username, String passwordHash, String fullName, String email, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    // Overloaded constructor for registration/creation prior to DB-assigned ID
    public User(String username, String passwordHash, String fullName, String email, Role role) {
        this(0, username, passwordHash, fullName, email, role, LocalDateTime.now());
    }

    // Polymorphic method overridden by subclasses
    public abstract String getRoleDisplayName();

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isStudent() {
        return this.role == Role.STUDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id == user.id && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " - " + role + ")";
    }
}
