package com.library.seatmgmt.controller;

import com.library.seatmgmt.exception.BookingConflictException;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.exception.SeatUnavailableException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.Floor;
import com.library.seatmgmt.model.Seat;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.service.BookingService;
import com.library.seatmgmt.service.RecommendationService;
import com.library.seatmgmt.service.SeatService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Floor-Wise Interactive Seat Map.
 * Displays 20 seats per floor in a neat 4x5 visual grid and coordinates booking transactions.
 */
public class SeatMapController {

    private static final int GRID_COLUMNS = 5; // 4 rows x 5 columns = 20 seats per floor

    @FXML private ComboBox<Floor> cmbFloor;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cmbStartTime;
    @FXML private ComboBox<String> cmbEndTime;

    @FXML private CheckBox chkPower;
    @FXML private CheckBox chkWindow;
    @FXML private CheckBox chkPc;

    @FXML private Label lblFloorHeader;
    @FXML private Label lblFloorDescription;
    @FXML private Label lblFloorLiveStats;
    @FXML private GridPane seatGridPane;

    // Booking Summary Drawer components
    @FXML private VBox drawerEmptyState;
    @FXML private VBox drawerContent;
    @FXML private Label lblSummarySeatNumber;
    @FXML private Label lblSummaryFloorName;
    @FXML private Label lblSummaryFloorShort;
    @FXML private Label lblSummaryDate;
    @FXML private Label lblSummaryTime;
    @FXML private Label lblSummaryDuration;
    @FXML private Label lblSummaryFacilities;
    @FXML private Button btnConfirmBooking;

    private final SeatService seatService = new SeatService();
    private final BookingService bookingService = new BookingService();
    private final RecommendationService recommendationService = new RecommendationService();

    private Seat selectedSeat = null;
    private VBox selectedTile = null;
    private final Map<Integer, VBox> seatTileMap = new HashMap<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US);

    @FXML
    public void initialize() {
        initTimeSlots();
        initDateAndFloors();
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

        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(9, 55))) {
            dpDate.setValue(LocalDate.now());
            cmbStartTime.setValue("10:00 AM");
            cmbEndTime.setValue("12:00 PM");
        } else if (now.isBefore(LocalTime.of(19, 0))) {
            dpDate.setValue(LocalDate.now());
            int startHour = Math.min(19, now.getHour() + 1);
            int endHour = Math.min(20, startHour + 2);
            cmbStartTime.setValue(LocalTime.of(startHour, 0).format(TIME_FMT));
            cmbEndTime.setValue(LocalTime.of(endHour, 0).format(TIME_FMT));
        } else {
            dpDate.setValue(LocalDate.now().plusDays(1));
            cmbStartTime.setValue("10:00 AM");
            cmbEndTime.setValue("12:00 PM");
        }
    }

    private void initDateAndFloors() {
        if (dpDate.getValue() == null) {
            dpDate.setValue(LocalDate.now());
        }

        try {
            List<Floor> floors = seatService.getAllActiveFloors();
            if (!floors.isEmpty()) {
                cmbFloor.setItems(FXCollections.observableArrayList(floors));
                cmbFloor.setValue(floors.get(0));
            }
        } catch (Exception e) {
            LoggerUtil.warning("Unable to fetch floors from database: " + e.getMessage());
        }

        cmbFloor.setOnAction(e -> handleRefreshGrid());
        dpDate.setOnAction(e -> handleRefreshGrid());
        cmbStartTime.setOnAction(e -> handleRefreshGrid());
        cmbEndTime.setOnAction(e -> handleRefreshGrid());
    }

    @FXML
    public void handleRefreshGrid() {
        selectedSeat = null;
        selectedTile = null;
        updateDrawer();

        Floor floor = cmbFloor.getValue();
        if (floor == null) return;

        lblFloorHeader.setText(floor.getFloorName());
        lblFloorDescription.setText(floor.getDescription() != null ? floor.getDescription() : "Floor section (20 seats)");

        LocalDate date = dpDate.getValue() != null ? dpDate.getValue() : LocalDate.now();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        seatGridPane.getChildren().clear();
        seatTileMap.clear();

        try {
            List<Seat> seats = seatService.getSeatsByFloor(floor.getFloorId());
            int avail = 0, res = 0, occ = 0, maint = 0;

            for (int idx = 0; idx < seats.size(); idx++) {
                Seat s = seats.get(idx);
                String status = seatService.getSeatStatusForSlot(s, date, start, end);
                switch (status) {
                    case "RESERVED" -> res++;
                    case "OCCUPIED" -> occ++;
                    case "MAINTENANCE" -> maint++;
                    default -> avail++;
                }

                VBox tile = createSeatTile(s, status);
                seatTileMap.put(s.getId(), tile);

                int col = idx % GRID_COLUMNS;
                int row = idx / GRID_COLUMNS;
                seatGridPane.add(tile, col, row);
            }

            lblFloorLiveStats.setText(String.format(
                    "%d Seats  •  Available: %d  |  Reserved: %d  |  Occupied: %d  |  Maintenance: %d",
                    seats.size(), avail, res, occ, maint));
        } catch (Exception e) {
            LoggerUtil.severe("Error rendering floor seat grid", e);
        }
    }

    private VBox createSeatTile(Seat seat, String status) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("seat-tile");

        switch (status) {
            case "RESERVED" -> tile.getStyleClass().add("seat-reserved");
            case "OCCUPIED" -> tile.getStyleClass().add("seat-occupied");
            case "MAINTENANCE" -> tile.getStyleClass().add("seat-maintenance");
            default -> tile.getStyleClass().add("seat-available");
        }

        Label lblNum = new Label(seat.getSeatNumber());
        lblNum.getStyleClass().add("seat-tile-number");

        Label lblSub = new Label("MAINTENANCE".equals(status) ? "🛠️ Maint." : seat.getFacilitiesDescription());
        lblSub.getStyleClass().add("seat-tile-icons");

        tile.getChildren().addAll(lblNum, lblSub);

        Tooltip tip = new Tooltip("Seat " + seat.getSeatNumber() +
                "\nFloor: " + (seat.getFloorName() != null ? seat.getFloorName() : "N/A") +
                "\nStatus: " + status +
                "\n" + seat.getFacilitiesDescription());
        Tooltip.install(tile, tip);

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

            Floor floor = cmbFloor.getValue();
            lblSummarySeatNumber.setText("Seat " + selectedSeat.getSeatNumber());
            lblSummaryFloorName.setText(floor != null ? floor.getFloorName() : "");
            lblSummaryFloorShort.setText(floor != null ? "Floor " + floor.getFloorNumber() : "--");
            lblSummaryDate.setText(dpDate.getValue() != null ? dpDate.getValue().toString() : LocalDate.now().toString());
            lblSummaryTime.setText(cmbStartTime.getValue() + " - " + cmbEndTime.getValue());

            LocalTime start = parseTime(cmbStartTime.getValue());
            LocalTime end = parseTime(cmbEndTime.getValue());
            long minutes = Duration.between(start, end).toMinutes();
            lblSummaryDuration.setText(minutes > 0 ? (minutes / 60) + " hr " + (minutes % 60) + " min" : "Invalid time range");

            lblSummaryFacilities.setText(selectedSeat.getFacilitiesDescription());
        }
    }

    @FXML
    public void handleSmartRecommend() {
        Floor floor = cmbFloor.getValue();
        if (floor == null) return;

        LocalDate date = dpDate.getValue() != null ? dpDate.getValue() : LocalDate.now();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        List<Seat> recommended = recommendationService.recommendSeats(
                floor.getFloorId(), date, start, end,
                chkPower.isSelected(), chkWindow.isSelected(), chkPc.isSelected()
        );

        if (recommended.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Match Found",
                    "No available seats match your criteria on " + floor.getFloorName() + " for the selected slot.");
        } else {
            Seat best = recommended.get(0);
            handleRefreshGrid();
            VBox bestTile = seatTileMap.get(best.getId());
            if (bestTile != null) {
                bestTile.getStyleClass().add("seat-selected");
                selectedTile = bestTile;
                selectedSeat = best;
                updateDrawer();
            }

            showAlert(Alert.AlertType.INFORMATION, "Smart Recommendation",
                    "Best matched seat on " + floor.getFloorName() + ": " + best.getSeatNumber() +
                    " (" + best.getFacilitiesDescription() + ")");
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

        Floor floor = cmbFloor.getValue();
        LocalDate date = dpDate.getValue() != null ? dpDate.getValue() : LocalDate.now();
        LocalTime start = parseTime(cmbStartTime.getValue());
        LocalTime end = parseTime(cmbEndTime.getValue());

        try {
            Booking booking = bookingService.createBooking(user.getId(), selectedSeat.getId(), date, start, end);
            String floorLabel = floor != null ? floor.getFloorName() : (booking.getFloorName() != null ? booking.getFloorName() : "Selected Floor");
            showAlert(Alert.AlertType.INFORMATION, "Reservation Confirmed!",
                    "Floor: " + floorLabel + "\n" +
                    "Seat Number: " + selectedSeat.getSeatNumber() + "\n" +
                    "Booking ID: #" + booking.getId() + "\n" +
                    "Date & Time: " + date + " (" + booking.getFormattedTimeSlot() + ")");

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
        try {
            return LocalTime.parse(formatted.trim(), TIME_FMT);
        } catch (Exception e) {
            try {
                String clean = formatted.trim().toUpperCase(java.util.Locale.US);
                boolean isPm = clean.endsWith("PM");
                String digits = clean.replaceAll("[^0-9:]", "");
                String[] parts = digits.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (isPm && hour < 12) hour += 12;
                if (!isPm && hour == 12) hour = 0;
                return LocalTime.of(hour, minute);
            } catch (Exception ex) {
                return LocalTime.of(10, 0);
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
