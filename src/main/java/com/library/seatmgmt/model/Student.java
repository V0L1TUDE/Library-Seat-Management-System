package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Entity representing a Student user.
 * Demonstrates Inheritance and Polymorphic method overriding.
 */
public class Student extends User {
    private static final long serialVersionUID = 1L;

    private String studentIdNumber; // e.g. STU-2024-001
    private String department;      // e.g. Computer Science
    private String phone;           // Contact number

    public Student() {
        super();
        setRole(Role.STUDENT);
    }

    public Student(int id, String username, String passwordHash, String fullName, String email,
                   LocalDateTime createdAt, String studentIdNumber, String department, String phone) {
        super(id, username, passwordHash, fullName, email, Role.STUDENT, createdAt);
        this.studentIdNumber = studentIdNumber;
        this.department = department;
        this.phone = phone;
    }

    public Student(String username, String passwordHash, String fullName, String email,
                   String studentIdNumber, String department, String phone) {
        super(username, passwordHash, fullName, email, Role.STUDENT);
        this.studentIdNumber = studentIdNumber;
        this.department = department;
        this.phone = phone;
    }

    @Override
    public String getRoleDisplayName() {
        return "Student (" + (department != null ? department : "General") + ")";
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
