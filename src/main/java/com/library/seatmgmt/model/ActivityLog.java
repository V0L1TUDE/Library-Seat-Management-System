package com.library.seatmgmt.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit log entry capturing critical system and security events.
 */
public class ActivityLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Integer userId;
    private String action;
    private String details;
    private LocalDateTime logTime;

    // Transient helper for UI display
    private transient String username;

    public ActivityLog() {
        this.logTime = LocalDateTime.now();
    }

    public ActivityLog(int id, Integer userId, String action, String details, LocalDateTime logTime) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.logTime = logTime != null ? logTime : LocalDateTime.now();
    }

    public ActivityLog(Integer userId, String action, String details) {
        this(0, userId, action, details, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "[" + logTime + "] " + action + ": " + details;
    }
}
