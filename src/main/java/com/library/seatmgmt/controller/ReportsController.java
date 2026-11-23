package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.util.FileUtil;
import com.library.seatmgmt.util.LoggerUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Reporting, Audit Logs, and CSV File Export.
 * Demonstrates File I/O and System Analytics.
 */
public class ReportsController {

    @FXML private Label lblTotalBookingsCount;
    @FXML private Label lblCompletedCount;
    @FXML private Label lblNoShowCount;

    @FXML private TableView<ActivityLog> tblActivityLogs;
    @FXML private TableColumn<ActivityLog, String> colLogId;
    @FXML private TableColumn<ActivityLog, String> colLogUser;
    @FXML private TableColumn<ActivityLog, String> colLogAction;
    @FXML private TableColumn<ActivityLog, String> colLogDetails;
    @FXML private TableColumn<ActivityLog, String> colLogTime;

    private final BookingDAO bookingDAO = new BookingDAOImpl();
    private final ActivityLogDAO logDAO = new ActivityLogDAOImpl();

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadReportData();
    }

    private void setupTableColumns() {
        colLogId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colLogUser.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUsername() != null ? d.getValue().getUsername() : "System"
        ));
        colLogAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction()));
        colLogDetails.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetails()));
        colLogTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getLogTime() != null ? d.getValue().getLogTime().format(LOG_FMT) : "--"
        ));
    }

    @FXML
    public void loadReportData() {
        try {
            int completed = bookingDAO.countByStatus(BookingStatus.COMPLETED);
            int noShows = bookingDAO.countByStatus(BookingStatus.NO_SHOW);
            int reserved = bookingDAO.countByStatus(BookingStatus.RESERVED);
            int occupied = bookingDAO.countByStatus(BookingStatus.OCCUPIED);
            int cancelled = bookingDAO.countByStatus(BookingStatus.CANCELLED);
            int total = completed + noShows + reserved + occupied + cancelled;

            lblTotalBookingsCount.setText(String.valueOf(total));
            lblCompletedCount.setText(String.valueOf(completed));
            lblNoShowCount.setText(String.valueOf(noShows));

            // Load audit logs
            List<ActivityLog> logs = logDAO.findRecentLogs(50);
            tblActivityLogs.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception e) {
            LoggerUtil.warning("Unable to fetch complete reporting data: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportCsv() {
        try {
            List<Booking> bookings = bookingDAO.findAllRecent(500);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "reports/library_bookings_" + timestamp + ".csv";

            boolean success = FileUtil.exportBookingsToCsv(bookings, fileName);
            if (success) {
                File exportedFile = new File(fileName);
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Report exported successfully!\nLocation: " + exportedFile.getAbsolutePath(), ButtonType.OK);
                alert.setHeaderText("CSV Export Complete");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to write CSV file to disk.", ButtonType.OK);
                alert.setHeaderText("Export Error");
                alert.showAndWait();
            }
        } catch (Exception e) {
            LoggerUtil.severe("CSV export error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unexpected error: " + e.getMessage(), ButtonType.OK);
            alert.setHeaderText("Export Error");
            alert.showAndWait();
        }
    }
}
