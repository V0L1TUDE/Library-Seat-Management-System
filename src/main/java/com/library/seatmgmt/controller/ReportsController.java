package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.service.SeatService;
import com.library.seatmgmt.util.FileUtil;
import com.library.seatmgmt.util.LoggerUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Reporting, Floor-Wise Occupancy Reports, Audit Logs, and CSV Export.
 */
public class ReportsController {

    @FXML private Label lblTotalBookingsCount;
    @FXML private Label lblCompletedCount;
    @FXML private Label lblNoShowCount;
    @FXML private Label lblActiveFloorsCount;

    @FXML private TableView<Floor> tblFloorReport;
    @FXML private TableColumn<Floor, String> colRepFloorNum;
    @FXML private TableColumn<Floor, String> colRepFloorName;
    @FXML private TableColumn<Floor, String> colRepTotal;
    @FXML private TableColumn<Floor, String> colRepAvail;
    @FXML private TableColumn<Floor, String> colRepRes;
    @FXML private TableColumn<Floor, String> colRepOcc;
    @FXML private TableColumn<Floor, String> colRepMaint;
    @FXML private TableColumn<Floor, String> colRepUtil;

    @FXML private TableView<ActivityLog> tblActivityLogs;
    @FXML private TableColumn<ActivityLog, String> colLogId;
    @FXML private TableColumn<ActivityLog, String> colLogUser;
    @FXML private TableColumn<ActivityLog, String> colLogAction;
    @FXML private TableColumn<ActivityLog, String> colLogDetails;
    @FXML private TableColumn<ActivityLog, String> colLogTime;

    private final BookingDAO bookingDAO = new BookingDAOImpl();
    private final ActivityLogDAO logDAO = new ActivityLogDAOImpl();
    private final SeatService seatService = new SeatService();

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadReportData();
    }

    private void setupTableColumns() {
        colRepFloorNum.setCellValueFactory(d -> new SimpleStringProperty("Floor " + d.getValue().getFloorNumber()));
        colRepFloorName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFloorName()));
        colRepTotal.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalSeats())));
        colRepAvail.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAvailableSeats())));
        colRepRes.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getReservedSeats())));
        colRepOcc.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOccupiedSeats())));
        colRepMaint.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMaintenanceSeats())));
        colRepUtil.setCellValueFactory(d -> {
            int total = d.getValue().getTotalSeats();
            int used = d.getValue().getReservedSeats() + d.getValue().getOccupiedSeats();
            double pct = total > 0 ? (used * 100.0 / total) : 0.0;
            return new SimpleStringProperty(String.format("%.1f%%", pct));
        });

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

            List<Floor> floorSummary = seatService.getFloorOccupancySummary(LocalDate.now(), LocalTime.MIN, LocalTime.MAX);
            lblActiveFloorsCount.setText(String.valueOf(floorSummary.size()));
            tblFloorReport.setItems(FXCollections.observableArrayList(floorSummary));

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
