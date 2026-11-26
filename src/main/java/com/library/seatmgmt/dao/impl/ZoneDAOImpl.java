package com.library.seatmgmt.dao.impl;

import com.library.seatmgmt.dao.FloorDAO;
import com.library.seatmgmt.dao.ZoneDAO;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.ZoneType;

import java.util.List;
import java.util.Optional;

/**
 * Compatibility adapter implementing ZoneDAO on top of FloorDAO.
 */
public class ZoneDAOImpl implements ZoneDAO {

    private final FloorDAO floorDAO = new FloorDAOImpl();

    @Override
    public Optional<Zone> findById(int id) {
        return floorDAO.findById(id).map(this::toZone);
    }

    @Override
    public Optional<Zone> findByCode(String code) {
        return floorDAO.findByFloorName(code).map(this::toZone);
    }

    @Override
    public List<Zone> findAll(boolean activeOnly) {
        return floorDAO.findAll(activeOnly).stream().map(this::toZone).toList();
    }

    @Override
    public boolean save(Zone zone) {
        Floor floor = new Floor(zone.getName(), floorDAO.countTotalFloors(), zone.getDescription());
        boolean saved = floorDAO.save(floor);
        if (saved) {
            zone.setId(floor.getFloorId());
        }
        return saved;
    }

    @Override
    public boolean update(Zone zone) {
        Optional<Floor> existing = floorDAO.findById(zone.getId());
        if (existing.isEmpty()) return false;
        Floor f = existing.get();
        f.setFloorName(zone.getName());
        f.setDescription(zone.getDescription());
        f.setActive(zone.isActive());
        return floorDAO.update(f);
    }

    @Override
    public boolean toggleActive(int zoneId, boolean active) {
        Optional<Floor> existing = floorDAO.findById(zoneId);
        if (existing.isEmpty()) return false;
        Floor f = existing.get();
        f.setActive(active);
        return floorDAO.update(f);
    }

    @Override
    public int countTotalZones() {
        return floorDAO.countTotalFloors();
    }

    private Zone toZone(Floor f) {
        ZoneType type = switch (f.getFloorNumber()) {
            case 1 -> ZoneType.QUIET;
            case 2 -> ZoneType.COMPUTER;
            case 3 -> ZoneType.COLLABORATIVE;
            default -> ZoneType.GENERAL;
        };
        return new Zone(f.getFloorId(), f.getFloorName(), "FLOOR-" + f.getFloorNumber(),
                f.getDescription(), type, f.isActive(), f.getCreatedAt());
    }
}
