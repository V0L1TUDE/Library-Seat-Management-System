package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.FloorDAO;
import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.UserDAO;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.dao.impl.FloorDAOImpl;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.dao.impl.UserDAOImpl;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.service.NoShowScannerService;
import com.library.seatmgmt.service.SeatService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for the Administrator Floor & Seat Management Dashboard.
 * Supports floor-wise occupancy monitoring, adding new floors (auto-creating 20 seats),
 * managing individual seat maintenance statuses, and inspecting student bookings.
 */
public class AdminDashboardController {

    @FXML private Label lblAdminTotalFloors;
    @FXML private Label lblAdminTotalSeats;
    @FXML private Label lblAdminTotalStudents;
    @FXML private Label lblAdminAvailable;
    @FXML private Label lblAdminReserved;
    @FXML private Label lblAdminOccupied;
    @FXML private Label lblAdminMaintenance;

    @FXML private PieChart chartOccupancy;

    // Tab 1: Add Floor + Floor-Wise Summary
    @FXML private TextField txtNewFloorName;
    @FXML private TextField txtNewFloorNumber;
    @FXML private TextField txtNewFloorDesc;

    @FXML private TableView<Floor> tblFloorSummary;
    @FXML private TableColumn<Floor, String> colFloorNum;
    @FXML private TableColumn<Floor, String> colFloorName;
    @FXML private TableColumn<Floor, String> colFloorTotal;
    @FXML private TableColumn<Floor, String> colFloorAvail;
    @FXML private TableColumn<Floor, String> colFloorRes;
    @FXML private TableColumn<Floor, String> colFloorOcc;
    @FXML private TableColumn<Floor, String> colFloorMaint;

    // Tab 2: Floor Seat Status Management
    @FXML private ComboBox<Floor> cmbAdminFloor;
    @FXML private Button btnSetAvailable;
    @FXML private Button btnSetMaintenance;

    @FXML private TableView<Seat> tblAdminSeats;
    @FXML private TableColumn<Seat, String> colSeatId;
    @FXML private TableColumn<Seat, String> colSeatNumber;
    @FXML private TableColumn<Seat, String> colSeatFloor;
    @FXML private TableColumn<Seat, String> colSeatStatus;
    @FXML private TableColumn<Seat, String> colSeatFacilities;

    // Tab 3: Student Bookings Monitor
    @FXML private ComboBox<Floor> cmbAdminBookingFloor;
    @FXML private TableView<Booking> tblAdminBookings;
    @FXML private TableColumn<Booking, String> colBkId;
    @FXML private TableColumn<Booking, String> colBkStudent;
    @FXML private TableColumn<Booking, String> colBkRoll;
    @FXML private TableColumn<Booking, String> colBkFloor;
    @FXML private TableColumn<Booking, String> colBkSeat;
    @FXML private TableColumn<Booking, String> colBkDate;
    @FXML private TableColumn<Booking, String> colBkTime;
    @FXML private TableColumn<Booking, String> colBkStatus;

    private final FloorDAO floorDAO = new FloorDAOImpl();
    private final SeatDAO seatDAO = new SeatDAOImpl();
    private final BookingDAO bookingDAO = new BookingDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final SeatService seatService = new SeatService();
    private final NoShowScannerService noShowScannerService = new NoShowScannerService();

    private static final Floor ALL_FLOORS_OPTION = new Floor(-1, "All Floors", -1, "", true, null);

    @FXML
    public void initialize() {
        setupFloorSummaryColumns();
        setupSeatTableColumns();
        setupBookingTableColumns();

        cmbAdminFloor.setOnAction(e -> refreshSeatsTable());
        cmbAdminBookingFloor.setOnAction(e -> refreshBookingsTable());

        tblAdminSeats.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSel = newSel != null;
            btnSetAvailable.setDisable(!hasSel || newSel.getStatus() == SeatStatus.AVAILABLE);
            btnSetMaintenance.setDisable(!hasSel || newSel.getStatus() == SeatStatus.MAINTENANCE);
        });

        loadAdminData();
    }

    private void setupFloorSummaryColumns() {
        colFloorNum.setCellValueFactory(d -> new SimpleStringProperty("Floor " + d.getValue().getFloorNumber()));
        colFloorName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFloorName()));
        colFloorTotal.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalSeats())));
        colFloorAvail.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAvailableSeats())));
        colFloorRes.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getReservedSeats())));
        colFloorOcc.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOccupiedSeats())));
        colFloorMaint.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMaintenanceSeats())));
    }

    private void setupSeatTableColumns() {
        colSeatId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colSeatNumber.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeatNumber()));
        colSeatFloor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFloorName()));
        colSeatStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
        colSeatFacilities.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFacilitiesDescription()));

        colSeatStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("Available".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-available");
                    else badge.getStyleClass().add("badge-maintenance");
                    setGraphic(badge);
                }
            }
        });
    }

    private void setupBookingTableColumns() {
        colBkId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colBkStudent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName() != null ? d.getValue().getStudentName() : "Student"));
        colBkRoll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentIdNumber() != null ? d.getValue().getStudentIdNumber() : "--"));
        colBkFloor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFloorName()));
        colBkSeat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeatNumber()));
        colBkDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingDate().toString()));
        colBkTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTimeSlot()));
        colBkStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
    }

    @FXML
    public void loadAdminData() {
        try {
            int totalFloors = floorDAO.countTotalFloors();
            int totalSeats = seatDAO.countTotalSeats();
            int maintenance = seatDAO.countSeatsByStatus(SeatStatus.MAINTENANCE);
            int occupied = bookingDAO.countByStatus(BookingStatus.OCCUPIED);
            int reserved = bookingDAO.countByStatus(BookingStatus.RESERVED);
            int available = Math.max(0, totalSeats - maintenance - occupied - reserved);
            int students = userDAO.countTotalStudents();

            lblAdminTotalFloors.setText(String.valueOf(totalFloors));
            lblAdminTotalSeats.setText(String.valueOf(totalSeats));
            lblAdminTotalStudents.setText(String.valueOf(students));
            lblAdminAvailable.setText(String.valueOf(available));
            lblAdminReserved.setText(String.valueOf(reserved));
            lblAdminOccupied.setText(String.valueOf(occupied));
            lblAdminMaintenance.setText(String.valueOf(maintenance));

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Available (" + available + ")", (totalSeats == 0 && occupied == 0 && reserved == 0 && maintenance == 0) ? 1 : available),
                    new PieChart.Data("Reserved (" + reserved + ")", reserved),
                    new PieChart.Data("Occupied (" + occupied + ")", occupied),
                    new PieChart.Data("Maintenance (" + maintenance + ")", maintenance)
            );
            chartOccupancy.setData(pieData);

            // Refresh Floor ComboBoxes
            List<Floor> floors = seatService.getAllFloors();
            ObservableList<Floor> floorFilterList = FXCollections.observableArrayList();
            floorFilterList.add(ALL_FLOORS_OPTION);
            floorFilterList.addAll(floors);

            Floor prevSeatFloor = cmbAdminFloor.getValue();
            cmbAdminFloor.setItems(floorFilterList);
            cmbAdminFloor.setValue(prevSeatFloor != null && floorFilterList.contains(prevSeatFloor) ? prevSeatFloor : ALL_FLOORS_OPTION);

            Floor prevBkFloor = cmbAdminBookingFloor.getValue();
            cmbAdminBookingFloor.setItems(floorFilterList);
            cmbAdminBookingFloor.setValue(prevBkFloor != null && floorFilterList.contains(prevBkFloor) ? prevBkFloor : ALL_FLOORS_OPTION);

            // Floor-wise occupancy breakdown
            List<Floor> summary = seatService.getFloorOccupancySummary(LocalDate.now(), LocalTime.MIN, LocalTime.MAX);
            tblFloorSummary.setItems(FXCollections.observableArrayList(summary));

            refreshSeatsTable();
            refreshBookingsTable();

        } catch (Exception e) {
            LoggerUtil.warning("Unable to fetch complete admin metrics: " + e.getMessage());
        }
    }

    private void refreshSeatsTable() {
        Floor selected = cmbAdminFloor.getValue();
        List<Seat> seats = (selected == null || selected.getFloorId() < 0)
                ? seatDAO.findAll()
                : seatDAO.findByFloorId(selected.getFloorId());
        tblAdminSeats.setItems(FXCollections.observableArrayList(seats));
    }

    private void refreshBookingsTable() {
        Floor selected = cmbAdminBookingFloor.getValue();
        List<Booking> bookings = (selected == null || selected.getFloorId() < 0)
                ? bookingDAO.findAllRecent(100)
                : bookingDAO.findAllRecentByFloor(selected.getFloorId(), 100);
        tblAdminBookings.setItems(FXCollections.observableArrayList(bookings));
    }

    @FXML
    public void handleAddFloor() {
        String name = txtNewFloorName.getText();
        String numStr = txtNewFloorNumber.getText();
        String desc = txtNewFloorDesc.getText();

        int floorNumber;
        try {
            floorNumber = Integer.parseInt(numStr != null ? numStr.trim() : "");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a valid numeric floor number (e.g., 4).");
            return;
        }

        User admin = SessionContext.getInstance().getCurrentUser();
        Integer adminId = admin != null ? admin.getId() : null;

        try {
            Floor created = seatService.addFloor(name, floorNumber, desc, adminId);
            txtNewFloorName.clear();
            txtNewFloorNumber.clear();
            txtNewFloorDesc.clear();

            showAlert(Alert.AlertType.INFORMATION, "Floor Created",
                    "Floor '" + created.getFloorName() + "' was added to MySQL.\n" +
                    "20 seats (S-01 to S-20) were automatically generated.");
            loadAdminData();
        } catch (InvalidBookingException e) {
            showAlert(Alert.AlertType.WARNING, "Cannot Create Floor", e.getMessage());
        }
    }

    @FXML
    public void handleSetSeatAvailable() {
        updateSelectedSeatStatus(SeatStatus.AVAILABLE);
    }

    @FXML
    public void handleSetSeatMaintenance() {
        updateSelectedSeatStatus(SeatStatus.MAINTENANCE);
    }

    private void updateSelectedSeatStatus(SeatStatus targetStatus) {
        Seat sel = tblAdminSeats.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        User admin = SessionContext.getInstance().getCurrentUser();
        Integer adminId = admin != null ? admin.getId() : null;

        if (seatService.updateSeatStatus(sel.getId(), targetStatus, adminId)) {
            showAlert(Alert.AlertType.INFORMATION, "Seat Status Updated",
                    sel.getFloorName() + " — Seat " + sel.getSeatNumber() + " is now " + targetStatus.getDisplayName() + ".");
            loadAdminData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update seat status in database.");
        }
    }

    @FXML
    public void handleTriggerNoShowScan() {
        int releasedCount = noShowScannerService.scanAndReleaseNoShows();
        showAlert(Alert.AlertType.INFORMATION, "No-Show Scan Executed",
                "Scan complete. " + releasedCount + " expired reservations were automatically released as NO-SHOW.");
        loadAdminData();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
