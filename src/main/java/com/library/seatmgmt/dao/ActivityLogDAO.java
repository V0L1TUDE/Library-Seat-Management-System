package com.library.seatmgmt.dao;

import com.library.seatmgmt.model.ActivityLog;
import java.util.List;

/**
 * Data Access Object interface for system audit logs.
 */
public interface ActivityLogDAO {
    boolean save(ActivityLog log);
    List<ActivityLog> findRecentLogs(int limit);
}
