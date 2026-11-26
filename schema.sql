-- ============================================================
-- Smart Library Seat Management System - Floor-Wise MySQL Schema
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

-- 2. FLOORS TABLE (Independent Floor-Wise Library Sections)
CREATE TABLE floors (
    floor_id INT AUTO_INCREMENT PRIMARY KEY,
    floor_name VARCHAR(80) NOT NULL UNIQUE,
    floor_number INT NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_floor_number (floor_number)
) ENGINE=InnoDB;

-- 3. SEATS TABLE (Exactly 20 Seats Per Floor)
CREATE TABLE seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    floor_id INT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    seat_status ENUM('AVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    has_power BOOLEAN NOT NULL DEFAULT FALSE,
    is_near_window BOOLEAN NOT NULL DEFAULT FALSE,
    has_pc BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_floor FOREIGN KEY (floor_id) REFERENCES floors(floor_id) ON DELETE CASCADE,
    CONSTRAINT uk_floor_seat UNIQUE (floor_id, seat_number),
    INDEX idx_seat_floor_status (floor_id, seat_status)
) ENGINE=InnoDB;

-- 4. BOOKINGS TABLE
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    seat_id INT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    booking_status ENUM('RESERVED', 'OCCUPIED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL DEFAULT 'RESERVED',
    check_in_time TIMESTAMP NULL DEFAULT NULL,
    check_out_time TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE RESTRICT,
    INDEX idx_booking_search (seat_id, booking_date, booking_status),
    INDEX idx_student_bookings (student_id, booking_date)
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
-- AUTOMATIC 20-SEAT PROVISIONING (PROCEDURE + TRIGGER)
-- Ensures every newly added floor automatically receives
-- exactly 20 unique seats (S-01 through S-20) without duplicates.
-- ============================================================

DELIMITER $$

CREATE PROCEDURE sp_create_seats_for_floor(IN p_floor_id INT, IN p_default_pc BOOLEAN)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_seat_num VARCHAR(20);
    DECLARE v_power BOOLEAN;
    DECLARE v_window BOOLEAN;
    DECLARE v_pc BOOLEAN;

    WHILE i <= 20 DO
        SET v_seat_num = CONCAT('S-', LPAD(i, 2, '0'));
        -- Practical realistic layout: outer edges near windows, most desks with power
        SET v_power = (i <= 16);
        SET v_window = (i IN (1, 5, 6, 10, 11, 15, 16, 20));
        SET v_pc = (p_default_pc OR (i <= 4));

        INSERT IGNORE INTO seats (floor_id, seat_number, seat_status, has_power, is_near_window, has_pc)
        VALUES (p_floor_id, v_seat_num, 'AVAILABLE', v_power, v_window, v_pc);

        SET i = i + 1;
    END WHILE;
END$$

CREATE TRIGGER trg_floors_auto_create_seats
AFTER INSERT ON floors
FOR EACH ROW
BEGIN
    CALL sp_create_seats_for_floor(NEW.floor_id, NEW.floor_number = 2);
END$$

DELIMITER ;

-- ============================================================
-- SEED DATA FOR TESTING AND EVALUATION
-- ============================================================

-- Passwords below are salted SHA-256 ('password123' + 'LMS_SECRET_SALT_2024'):
-- 8681faffa94ded79aae8812af199a8c2311441d8d4ea18da868b88651de0c5d0

INSERT IGNORE INTO users (username, password_hash, full_name, email, role, student_id_number, department, phone) VALUES
('admin', '8681faffa94ded79aae8812af199a8c2311441d8d4ea18da868b88651de0c5d0', 'Dr. Sarah Jenkins', 'admin@library.edu', 'ADMIN', NULL, 'Library Administration', '555-0100'),
('student1', '8681faffa94ded79aae8812af199a8c2311441d8d4ea18da868b88651de0c5d0', 'Alex Rivera', 'alex@student.edu', 'STUDENT', 'STU-2024-001', 'Computer Science', '555-0101'),
('student2', '8681faffa94ded79aae8812af199a8c2311441d8d4ea18da868b88651de0c5d0', 'Emily Chen', 'emily@student.edu', 'STUDENT', 'STU-2024-002', 'Electrical Engineering', '555-0102');

-- Insert 4 Floors (Trigger automatically creates 20 seats per floor = 80 seats total)
INSERT IGNORE INTO floors (floor_name, floor_number, description) VALUES
('Ground Floor — Section 1', 0, 'Open General Study Hall & Quick Access Circulation Desks (Seats 1 to 20).'),
('First Floor — Section 2', 1, 'Silent Reading Gallery & Individual Focused Study Pods (Seats 1 to 20).'),
('Second Floor — Section 3', 2, 'Digital Research Lab Equipped with Desktop PC Terminals (Seats 1 to 20).'),
('Third Floor — Section 4', 3, 'Collaborative Study Wing & Academic Project Tables (Seats 1 to 20).');

-- Seed sample bookings for today so live occupancy & dashboard charts display rich state
INSERT INTO bookings (student_id, seat_id, booking_date, start_time, end_time, booking_status, check_in_time)
SELECT u.id, s.seat_id, CURDATE(), '08:00:00', '20:00:00', 'RESERVED', NULL
FROM users u JOIN seats s ON s.floor_id = 1 AND s.seat_number = 'S-02'
WHERE u.username = 'student2';

INSERT INTO bookings (student_id, seat_id, booking_date, start_time, end_time, booking_status, check_in_time)
SELECT u.id, s.seat_id, CURDATE(), '08:00:00', '20:00:00', 'OCCUPIED', NOW()
FROM users u JOIN seats s ON s.floor_id = 2 AND s.seat_number = 'S-03'
WHERE u.username = 'student2';

INSERT INTO bookings (student_id, seat_id, booking_date, start_time, end_time, booking_status, check_in_time)
SELECT u.id, s.seat_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '12:00:00', 'RESERVED', NULL
FROM users u JOIN seats s ON s.floor_id = 1 AND s.seat_number = 'S-01'
WHERE u.username = 'student1';

INSERT INTO activity_logs (user_id, action, details) VALUES
(1, 'SYSTEM_INIT', 'Floor-wise database schema initialized with 4 floors and 80 seats (20 per floor).'),
(2, 'BOOKING_CREATED', 'Sample booking placed for Ground Floor — Section 1, Seat S-01.');
