package com.library.seatmgmt.model;

import com.library.seatmgmt.model.enums.ZoneType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a library zone/section with specific attributes and rules.
 */
public class Zone implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String code;
    private String description;
    private ZoneType zoneType;
    private boolean active;
    private LocalDateTime createdAt;

    // Transient runtime field for dashboard stats
    private transient int totalSeats;
    private transient int availableSeats;

    public Zone() {
        this.active = true;
    }

    public Zone(int id, String name, String code, String description, ZoneType zoneType, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.zoneType = zoneType;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Zone(String name, String code, String description, ZoneType zoneType) {
        this(0, name, code, description, zoneType, true, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public void setZoneType(ZoneType zoneType) {
        this.zoneType = zoneType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Zone zone)) return false;
        return id == zone.id && Objects.equals(code, zone.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
