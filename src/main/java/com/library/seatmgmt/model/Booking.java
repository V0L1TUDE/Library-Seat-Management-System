package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.BookingStatus;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a reservation placed by a student for a specific seat and time window.
 */
public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private int seatId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields for UI and table joins
    private transient String studentName;
    private transient String studentIdNumber;
    private transient String seatNumber;
    private transient int floorId;
    private transient String floorName;
    private transient int floorNumber;

    public Booking() {
        this.status = BookingStatus.RESERVED;
    }

    public Booking(int id, int userId, int seatId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                   BookingStatus status, LocalDateTime checkInTime, LocalDateTime checkOutTime,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.seatId = seatId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : BookingStatus.RESERVED;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public Booking(int userId, int seatId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        this(0, userId, seatId, bookingDate, startTime, endTime, BookingStatus.RESERVED,
             null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Determines whether this booking overlaps with a requested slot.
     * Core Algorithm: Overlap <=> (NewStart < ExistingEnd) AND (NewEnd > ExistingStart)
     */
    public boolean isOverlappingWith(LocalDate date, LocalTime targetStart, LocalTime targetEnd) {
        if (!Objects.equals(this.bookingDate, date)) {
            return false;
        }
        if (!this.status.isActive()) {
            return false;
        }
        return targetStart.isBefore(this.endTime) && targetEnd.isAfter(this.startTime);
    }

    /**
     * Checks if this booking is currently eligible for check-in.
     * Rule: Allowed starting 15 minutes before slot start up to 15 minutes after (grace period).
     */
    public boolean isEligibleForCheckIn(LocalDateTime now) {
        if (this.status != BookingStatus.RESERVED) {
            return false;
        }
        LocalDateTime slotStart = LocalDateTime.of(bookingDate, startTime);
        LocalDateTime earlyWindow = slotStart.minusMinutes(15);
        LocalDateTime graceDeadline = slotStart.plusMinutes(15);

        return !now.isBefore(earlyWindow) && !now.isAfter(graceDeadline);
    }

    /**
     * Checks if the grace period has passed without the student checking in.
     */
    public boolean isNoShow(LocalDateTime now) {
        if (this.status != BookingStatus.RESERVED) {
            return false;
        }
        LocalDateTime graceDeadline = LocalDateTime.of(bookingDate, startTime).plusMinutes(15);
        return now.isAfter(graceDeadline);
    }

    /**
     * Calculates duration in minutes.
     */
    public long getDurationInMinutes() {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toMinutes();
    }

    public String getFormattedTimeSlot() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US);
        return startTime.format(timeFmt) + " - " + endTime.format(timeFmt);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getFloorId() {
        return floorId;
    }

    public void setFloorId(int floorId) {
        this.floorId = floorId;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getZoneName() {
        return floorName;
    }

    public void setZoneName(String zoneName) {
        this.floorName = zoneName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id == booking.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Booking #" + id + " [" + bookingDate + " " + startTime + "-" + endTime + " : " + status + "]";
    }
}
