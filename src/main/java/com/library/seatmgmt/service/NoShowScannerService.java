package com.library.seatmgmt.service;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.util.LoggerUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automated Scanner Service for No-Show Detection.
 * Identifies reservations where grace period has passed without check-in,
 * marks them as NO_SHOW, and releases the seats back to available.
 */
public class NoShowScannerService {

    private final BookingDAO bookingDAO;
    private final ActivityLogDAO logDAO;

    public NoShowScannerService() {
        this(new BookingDAOImpl(), new ActivityLogDAOImpl());
    }

    public NoShowScannerService(BookingDAO bookingDAO, ActivityLogDAO logDAO) {
        this.bookingDAO = bookingDAO;
        this.logDAO = logDAO;
    }

    /**
     * Executes a scan cycle against active reservations.
     * Returns count of expired bookings marked as NO_SHOW.
     */
    public int scanAndReleaseNoShows() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingDAO.findExpiredReservations(now);
        int count = 0;

        for (Booking b : expired) {
            boolean updated = bookingDAO.updateStatus(b.getId(), BookingStatus.NO_SHOW);
            if (updated) {
                count++;
                logDAO.save(new ActivityLog(b.getUserId(), "NO_SHOW_AUTO_RELEASE",
                        "Booking #" + b.getId() + " auto-released as NO-SHOW (grace period exceeded)."));
                LoggerUtil.info("Booking #" + b.getId() + " auto-released as NO_SHOW.");
            }
        }

        return count;
    }
}
