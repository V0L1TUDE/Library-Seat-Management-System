package com.library.seatmgmt.dao;

import com.library.seatmgmt.model.Zone;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Zone management.
 */
public interface ZoneDAO {
    Optional<Zone> findById(int id);
    Optional<Zone> findByCode(String code);
    List<Zone> findAll(boolean activeOnly);
    boolean save(Zone zone);
    boolean update(Zone zone);
    boolean toggleActive(int zoneId, boolean active);
    int countTotalZones();
}
