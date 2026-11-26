package com.library.seatmgmt.dao;

import com.library.seatmgmt.model.Floor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Floor-wise Library Section management.
 */
public interface FloorDAO {
    Optional<Floor> findById(int floorId);
    Optional<Floor> findByFloorNumber(int floorNumber);
    Optional<Floor> findByFloorName(String floorName);
    List<Floor> findAll(boolean activeOnly);
    boolean save(Floor floor);
    boolean update(Floor floor);
    int countTotalFloors();
    List<Floor> getFloorOccupancySummary(LocalDate date, LocalTime start, LocalTime end);
}
