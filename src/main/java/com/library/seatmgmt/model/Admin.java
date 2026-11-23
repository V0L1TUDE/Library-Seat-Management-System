package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Entity representing an Administrator user.
 * Demonstrates Inheritance and Polymorphic method overriding.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String department;

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int id, String username, String passwordHash, String fullName, String email,
                 LocalDateTime createdAt, String department) {
        super(id, username, passwordHash, fullName, email, Role.ADMIN, createdAt);
        this.department = department;
    }

    public Admin(String username, String passwordHash, String fullName, String email, String department) {
        super(username, passwordHash, fullName, email, Role.ADMIN);
        this.department = department;
    }

    @Override
    public String getRoleDisplayName() {
        return "Library Administrator";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
