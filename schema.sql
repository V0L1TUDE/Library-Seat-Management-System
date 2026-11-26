-- ============================================================
-- Smart Library Seat Management System Database Schema
-- Compatible with MySQL 8.0+
-- ============================================================

DROP DATABASE IF EXISTS library_seat_db;
CREATE DATABASE library_seat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_seat_db;

-- 1. USERS TABLE
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    student_id_number VARCHAR(50) UNIQUE NULL,
    department VARCHAR(80) NULL,
    phone VARCHAR(20) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- 2. ZONES TABLE
CREATE TABLE zones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    zone_type ENUM('QUIET', 'COMPUTER', 'COLLABORATIVE', 'GENERAL') NOT NULL DEFAULT 'GENERAL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. SEATS TABLE
CREATE TABLE seats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    zone_id INT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    has_power BOOLEAN NOT NULL DEFAULT FALSE,
    is_near_window BOOLEAN NOT NULL DEFAULT FALSE,
    has_pc BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('AVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE,
    CONSTRAINT uk_zone_seat UNIQUE (zone_id, seat_number),
    INDEX idx_seat_zone_status (zone_id, status)
) ENGINE=InnoDB;

-- 4. BOOKINGS TABLE
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    seat_id INT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('RESERVED', 'OCCUPIED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL DEFAULT 'RESERVED',
    check_in_time TIMESTAMP NULL DEFAULT NULL,
    check_out_time TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE RESTRICT,
    INDEX idx_booking_search (seat_id, booking_date, status),
    INDEX idx_user_bookings (user_id, booking_date)
) ENGINE=InnoDB;

-- 5. ACTIVITY LOGS TABLE
CREATE TABLE activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_log_time (log_time)
) ENGINE=InnoDB;

-- ============================================================
-- SEED DATA FOR TESTING AND EVALUATION
-- ============================================================

-- Passwords below are SHA-256 for 'password123':
-- ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f

INSERT INTO users (username, password_hash, full_name, email, role, student_id_number, department, phone) VALUES
('admin', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Dr. Sarah Jenkins', 'admin@library.edu', 'ADMIN', NULL, 'Library Administration', '555-0100'),
('student1', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Alex Rivera', 'alex@student.edu', 'STUDENT', 'STU-2024-001', 'Computer Science', '555-0101'),
('student2', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Emily Chen', 'emily@student.edu', 'STUDENT', 'STU-2024-002', 'Electrical Engineering', '555-0102');

-- Seed Zones
INSERT INTO zones (name, code, description, zone_type) VALUES
('Silent Study Gallery', 'ZONE-A', 'Absolute silence zone with individual reading pods.', 'QUIET'),
('Digital Research Lab', 'ZONE-B', 'Equipped with PCs and high-speed research terminals.', 'COMPUTER'),
('Discussion & Group Hub', 'ZONE-C', 'Collaborative area for group study and projects.', 'COLLABORATIVE'),
('Main Hall General Seating', 'ZONE-D', 'Open layout tables near central circulation desk.', 'GENERAL');

-- Seed Seats for ZONE-A (Silent Study: Quiet, mostly near windows, all have power sockets)
INSERT INTO seats (zone_id, seat_number, has_power, is_near_window, has_pc, status) VALUES
(1, 'A-01', TRUE, TRUE, FALSE, 'AVAILABLE'),
(1, 'A-02', TRUE, TRUE, FALSE, 'AVAILABLE'),
(1, 'A-03', TRUE, FALSE, FALSE, 'AVAILABLE'),
(1, 'A-04', TRUE, FALSE, FALSE, 'AVAILABLE'),
(1, 'A-05', TRUE, TRUE, FALSE, 'AVAILABLE');

-- Seed Seats for ZONE-B (Digital Lab: All have PCs and power)
INSERT INTO seats (zone_id, seat_number, has_power, is_near_window, has_pc, status) VALUES
(2, 'B-01', TRUE, FALSE, TRUE, 'AVAILABLE'),
(2, 'B-02', TRUE, FALSE, TRUE, 'AVAILABLE'),
(2, 'B-03', TRUE, FALSE, TRUE, 'AVAILABLE'),
(2, 'B-04', TRUE, TRUE, TRUE, 'AVAILABLE');

-- Seed Seats for ZONE-C (Collaborative: Group tables)
INSERT INTO seats (zone_id, seat_number, has_power, is_near_window, has_pc, status) VALUES
(3, 'C-01', TRUE, TRUE, FALSE, 'AVAILABLE'),
(3, 'C-02', TRUE, FALSE, FALSE, 'AVAILABLE'),
(3, 'C-03', FALSE, FALSE, FALSE, 'AVAILABLE');

-- Seed Seats for ZONE-D (Main Hall)
INSERT INTO seats (zone_id, seat_number, has_power, is_near_window, has_pc, status) VALUES
(4, 'D-01', FALSE, FALSE, FALSE, 'AVAILABLE'),
(4, 'D-02', FALSE, FALSE, FALSE, 'AVAILABLE'),
(4, 'D-03', TRUE, TRUE, FALSE, 'AVAILABLE');
