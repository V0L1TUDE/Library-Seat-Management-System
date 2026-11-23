package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.exception.SeatUnavailableException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.Zone;
import com.library.seatmgmt.model.enums.SeatStatus;
import com.library.seatmgmt.service.BookingService;
import com.library.seatmgmt.service.RecommendationService;
import com.library.seatmgmt.service.SeatService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Interactive Seat Map.
 * Renders floor layout as dynamic visual seat pods and coordinates booking transactions.
 */
public class SeatMapController {

    @FXML private ComboBox<Zone> cmbZone;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cmbStartTime;
    @FXML private ComboBox<String> cmbEndTime;

    @FXML private CheckBox chkPower;
    @FXML private CheckBox chkWindow;
    @FXML private CheckBox chkPc;

    @FXML private Label lblZoneHeader;
    @FXML private Label lblZoneDescription;
    @FXML private FlowPane seatGridPane;

    // Drawer components
    @FXML private VBox drawerEmptyState;
    @FXML private VBox drawerContent;
    @FXML private Label lblSummarySeatNumber;
    @FXML private Label lblSummaryZoneName;
    @FXML private Label lblSummaryDate;
    @FXML private Label lblSummaryTime;
    @FXML private Label lblSummaryDuration;
    @FXML private Label lblSummaryFacilities;
    @FXML private Button btnConfirmBooking;

    private final SeatService seatService = new SeatService();
    private final BookingService bookingService = new BookingService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final SeatDAO seatDAO = new SeatDAOImpl();

    private Seat selectedSeat = null;
    private VBox selectedTile = null;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @FXML
    public void initialize() {
        initTimeSlots();
        initDateAndZones();
        handleRefreshGrid();
    }

    private void initTimeSlots() {
        List<String> times = List.of(
                "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
                "06:00 PM", "07:00 PM", "08:00 PM"
        );
        cmbStartTime.setItems(FXCollections.observableArrayList(times));
        cmbEndTime.setItems(FXCollections.observableArrayList(times));

        // Set sensible defaults: 10:00 AM to 12:00 PM
        cmbStartTime.setValue("10:00 AM");
        cmbEndTime.setValue("12:00 PM");
    }

    private void initDateAndZones() {
        dpDate.setValue(LocalDate.now());

        try {
            List<Zone> zones = seatService.getAllActiveZones();
            if (!zones.isEmpty()) {
                cmbZone.setItems(FXCollections.observableArrayList(zones));
                cmbZone.setValue(zones.get(0));
            }
        } catch (Exception e) {
            LoggerUtil.warning("Unable to fetch zones from database: " + e.getMessage());
        }

        cmbZone.setOnAction(e -> handleRefreshGrid());
    }

    @FXML
    public void handleRefreshGrid() {
        selectedSeat = null;
        updateDrawer();

        Zone zone = cmbZone.getValue();
        if (zone == null) return;

        lblZoneHeader.setText(zone.getName());
        lblZoneDescription.setText(zone.getDescription() != null ? zone.getDescription() : "Standard seating area");

        LocalDate date = dpDate.getValue();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        seatGridPane.getChildren().clear();

        try {
            List<Seat> seats = seatService.getSeatsByZone(zone.getId());
            for (Seat s : seats) {
                VBox tile = createSeatTile(s, date, start, end);
                seatGridPane.getChildren().add(tile);
            }
        } catch (Exception e) {
            LoggerUtil.severe("Error rendering seat grid", e);
        }
    }

    private VBox createSeatTile(Seat seat, LocalDate date, LocalTime start, LocalTime end) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("seat-tile");

        // Dynamic State check
        String status = seatService.getSeatStatusForSlot(seat, date, start, end);

        switch (status) {
            case "RESERVED" -> tile.getStyleClass().add("seat-reserved");
            case "OCCUPIED" -> tile.getStyleClass().add("seat-occupied");
            default -> tile.getStyleClass().add("seat-available");
        }

        Label lblNum = new Label(seat.getSeatNumber());
        lblNum.getStyleClass().add("seat-tile-number");

        Label lblIcons = new Label(seat.getFacilitiesDescription());
        lblIcons.getStyleClass().add("seat-tile-icons");

        tile.getChildren().addAll(lblNum, lblIcons);

        // Tooltip with details
        Tooltip tip = new Tooltip("Seat " + seat.getSeatNumber() + "\nStatus: " + status + "\n" + seat.getFacilitiesDescription());
        Tooltip.install(tile, tip);

        // Click Handler (Only if Available)
        if ("AVAILABLE".equals(status)) {
            tile.setOnMouseClicked(e -> {
                if (selectedTile != null) {
                    selectedTile.getStyleClass().remove("seat-selected");
                }
                tile.getStyleClass().add("seat-selected");
                selectedTile = tile;
                selectedSeat = seat;
                updateDrawer();
            });
        }

        return tile;
    }

    private void updateDrawer() {
        if (selectedSeat == null) {
            drawerEmptyState.setVisible(true);
            drawerEmptyState.setManaged(true);
            drawerContent.setVisible(false);
            drawerContent.setManaged(false);
        } else {
            drawerEmptyState.setVisible(false);
            drawerEmptyState.setManaged(false);
            drawerContent.setVisible(true);
            drawerContent.setManaged(true);

            lblSummarySeatNumber.setText(selectedSeat.getSeatNumber());
            lblSummaryZoneName.setText(cmbZone.getValue() != null ? cmbZone.getValue().getName() : "");
            lblSummaryDate.setText(dpDate.getValue().toString());
            lblSummaryTime.setText(cmbStartTime.getValue() + " - " + cmbEndTime.getValue());

            LocalTime start = parseTime(cmbStartTime.getValue());
            LocalTime end = parseTime(cmbEndTime.getValue());
            long minutes = Duration.between(start, end).toMinutes();
            lblSummaryDuration.setText((minutes / 60) + " hr " + (minutes % 60) + " min");

            lblSummaryFacilities.setText(selectedSeat.getFacilitiesDescription());
        }
    }

    @FXML
    public void handleSmartRecommend() {
        Zone zone = cmbZone.getValue();
        if (zone == null) return;

        LocalDate date = dpDate.getValue();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        List<Seat> recommended = recommendationService.recommendSeats(
                zone.getId(), date, start, end,
                chkPower.isSelected(), chkWindow.isSelected(), chkPc.isSelected()
        );

        if (recommended.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No available seats match your criteria in this zone for the selected time slot.", ButtonType.OK);
            alert.setHeaderText("No Match Found");
            alert.showAndWait();
        } else {
            Seat best = recommended.get(0);
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Best matched seat for your preferences: " + best.getSeatNumber() +
                    " (" + best.getFacilitiesDescription() + ")", ButtonType.OK);
            alert.setHeaderText("Smart Recommendation");
            alert.showAndWait();

            // Refresh and select best seat
            handleRefreshGrid();
        }
    }

    @FXML
    public void handleConfirmBooking() {
        if (selectedSeat == null) return;

        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Authentication Required", "Please log in to reserve a seat.");
            return;
        }

        LocalDate date = dpDate.getValue();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        try {
            Booking booking = bookingService.createBooking(user.getId(), selectedSeat.getId(), date, start, end);
            showAlert(Alert.AlertType.INFORMATION, "Reservation Confirmed!",
                    "Seat " + selectedSeat.getSeatNumber() + " reserved successfully.\n" +
                    "Booking ID: #" + booking.getId() + "\nTime: " + booking.getFormattedTimeSlot());

            handleRefreshGrid();
        } catch (BookingConflictException | SeatUnavailableException | InvalidBookingException e) {
            showAlert(Alert.AlertType.WARNING, "Booking Notice", e.getMessage());
        } catch (Exception e) {
            LoggerUtil.severe("Failed to reserve seat", e);
            showAlert(Alert.AlertType.ERROR, "System Error", "Failed to complete reservation. Please try again.");
        }
    }

    private LocalTime parseTime(String formatted) {
        if (formatted == null) return LocalTime.of(10, 0);
        return LocalTime.parse(formatted, TIME_FMT);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
