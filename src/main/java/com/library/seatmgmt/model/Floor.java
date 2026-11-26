package com.library.seatmgmt.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an independent library floor section containing 20 seats.
 */
public class Floor implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SEATS_PER_FLOOR = 20;

    private int floorId;
    private String floorName;
    private int floorNumber;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    // Transient runtime statistics for floor-wise occupancy monitoring
    private transient int totalSeats;
    private transient int availableSeats;
    private transient int reservedSeats;
    private transient int occupiedSeats;
    private transient int maintenanceSeats;

    public Floor() {
        this.active = true;
    }

    public Floor(int floorId, String floorName, int floorNumber, String description, boolean active, LocalDateTime createdAt) {
        this.floorId = floorId;
        this.floorName = floorName;
        this.floorNumber = floorNumber;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Floor(String floorName, int floorNumber, String description) {
        this(0, floorName, floorNumber, description, true, LocalDateTime.now());
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getReservedSeats() {
        return reservedSeats;
    }

    public void setReservedSeats(int reservedSeats) {
        this.reservedSeats = reservedSeats;
    }

    public int getOccupiedSeats() {
        return occupiedSeats;
    }

    public void setOccupiedSeats(int occupiedSeats) {
        this.occupiedSeats = occupiedSeats;
    }

    public int getMaintenanceSeats() {
        return maintenanceSeats;
    }

    public void setMaintenanceSeats(int maintenanceSeats) {
        this.maintenanceSeats = maintenanceSeats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Floor floor)) return false;
        return floorId == floor.floorId && floorNumber == floor.floorNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floorId, floorNumber);
    }

    @Override
    public String toString() {
        return floorName;
    }
}
