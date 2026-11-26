package com.library.seatmgmt.dao;

import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.enums.SeatStatus;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Seat management.
 */
public interface SeatDAO {
    Optional<Seat> findById(int id);
    Optional<Seat> findByFloorAndNumber(int floorId, String seatNumber);
    Optional<Seat> findByZoneAndNumber(int zoneId, String seatNumber);
    List<Seat> findByFloorId(int floorId);
    List<Seat> findByZoneId(int zoneId);
    List<Seat> findAll();
    boolean save(Seat seat);
    boolean update(Seat seat);
    boolean updateStatus(int seatId, SeatStatus status);
    int countTotalSeats();
    int countSeatsByStatus(SeatStatus status);
}
