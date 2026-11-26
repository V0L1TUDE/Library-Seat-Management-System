package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.SeatStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an individual library seating desk with facilities.
 */
public class Seat implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int floorId;
    private String seatNumber;        // e.g. "S-01" .. "S-20"
    private boolean hasPower;          // AC power socket available
    private boolean nearWindow;        // Natural light / window view
    private boolean hasPc;             // Desktop computer provided
    private SeatStatus status;         // AVAILABLE
    private LocalDateTime createdAt;

    // Transient runtime fields for UI display
    private transient String floorName;
    private transient int floorNumber;

    public Seat() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Seat(int id, int floorId, String seatNumber, boolean hasPower, boolean nearWindow,
                boolean hasPc, SeatStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.floorId = floorId;
        this.seatNumber = seatNumber;
        this.hasPower = hasPower;
        this.nearWindow = nearWindow;
        this.hasPc = hasPc;
        this.status = status != null ? status : SeatStatus.AVAILABLE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Seat(int floorId, String seatNumber, boolean hasPower, boolean nearWindow, boolean hasPc) {
        this(0, floorId, seatNumber, hasPower, nearWindow, hasPc, SeatStatus.AVAILABLE, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFloorId() {
        return floorId;
    }

    public void setFloorId(int floorId) {
        this.floorId = floorId;
    }

    public int getZoneId() {
        return floorId;
    }

    public void setZoneId(int zoneId) {
        this.floorId = zoneId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public boolean isHasPower() {
        return hasPower;
    }

    public void setHasPower(boolean hasPower) {
        this.hasPower = hasPower;
    }

    public boolean isNearWindow() {
        return nearWindow;
    }

    public void setNearWindow(boolean nearWindow) {
        this.nearWindow = nearWindow;
    }

    public boolean isHasPc() {
        return hasPc;
    }

    public void setHasPc(boolean hasPc) {
        this.hasPc = hasPc;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getZoneCode() {
        return "FLOOR-" + floorNumber;
    }

    public void setZoneCode(String zoneCode) {
        // Maintained for backward compatibility
    }

    /**
     * Formats features for human-readable UI tooltips.
     */
    public String getFacilitiesDescription() {
        StringBuilder sb = new StringBuilder();
        if (hasPower) sb.append("🔌 Power Socket ");
        if (nearWindow) sb.append("🪟 Near Window ");
        if (hasPc) sb.append("🖥️ PC Terminal ");
        if (sb.length() == 0) sb.append("Standard Desk");
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return id == seat.id && floorId == seat.floorId && Objects.equals(seatNumber, seat.seatNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, floorId, seatNumber);
    }

    @Override
    public String toString() {
        return seatNumber + (floorName != null ? " (" + floorName + ")" : "");
    }
}
