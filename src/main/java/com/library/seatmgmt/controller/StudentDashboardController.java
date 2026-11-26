package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.SeatDAO;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.dao.impl.SeatDAOImpl;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.service.BookingService;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for the Student Dashboard screen.
 */
public class StudentDashboardController {

    @FXML private Label lblTotalSeats;
    @FXML private Label lblAvailableSeats;
    @FXML private Label lblOccupiedSeats;
    @FXML private Label lblMyActiveBookings;

    @FXML private Label lblNextBookingBadge;
    @FXML private Label lblNextSeatNumber;
    @FXML private Label lblNextZoneName;
    @FXML private Label lblNextTimeSlot;
    @FXML private Button btnQuickCheckIn;

    @FXML private TableView<Booking> tblRecentBookings;
    @FXML private TableColumn<Booking, String> colId;
    @FXML private TableColumn<Booking, String> colSeat;
    @FXML private TableColumn<Booking, String> colZone;
    @FXML private TableColumn<Booking, String> colDate;
    @FXML private TableColumn<Booking, String> colTime;
    @FXML private TableColumn<Booking, String> colStatus;

    private final SeatDAO seatDAO = new SeatDAOImpl();
    private final BookingDAO bookingDAO = new BookingDAOImpl();
    private final BookingService bookingService = new BookingService();

    private Booking nextActiveBooking = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDashboardData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        colSeat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatNumber()));
        colZone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getZoneName()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingDate().toString()));
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTimeSlot()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));

        // Custom status badge rendering
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("Reserved".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-reserved");
                    else if ("Occupied".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-occupied");
                    else if ("Completed".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-completed");
                    else badge.getStyleClass().add("badge-cancelled");
                    setGraphic(badge);
                }
            }
        });
    }

    @FXML
    public void loadDashboardData() {
        try {
            int total = seatDAO.countTotalSeats();
            int occupied = bookingDAO.countByStatus(BookingStatus.OCCUPIED);
            int reserved = bookingDAO.countByStatus(BookingStatus.RESERVED);
            int available = Math.max(0, total - occupied - reserved);

            lblTotalSeats.setText(String.valueOf(total));
            lblAvailableSeats.setText(String.valueOf(available));
            lblOccupiedSeats.setText(String.valueOf(occupied));

            User user = SessionContext.getInstance().getCurrentUser();
            if (user != null) {
                List<Booking> active = bookingDAO.findActiveByUserId(user.getId());
                lblMyActiveBookings.setText(String.valueOf(active.size()));

                if (!active.isEmpty()) {
                    nextActiveBooking = active.get(0);
                    lblNextSeatNumber.setText(nextActiveBooking.getSeatNumber());
                    lblNextZoneName.setText(nextActiveBooking.getZoneName() != null ? nextActiveBooking.getZoneName() : "General Zone");
                    lblNextTimeSlot.setText(nextActiveBooking.getFormattedTimeSlot());
                    lblNextBookingBadge.setText(nextActiveBooking.getStatus().name());

                    // Check in eligibility
                    btnQuickCheckIn.setDisable(!nextActiveBooking.isEligibleForCheckIn(LocalDateTime.now()));
                } else {
                    lblNextSeatNumber.setText("No active bookings");
                    lblNextZoneName.setText("--");
                    lblNextTimeSlot.setText("--");
                    lblNextBookingBadge.setText("NONE");
                    btnQuickCheckIn.setDisable(true);
                }

                // Table data
                List<Booking> allUserBookings = bookingDAO.findByUserId(user.getId());
                tblRecentBookings.setItems(FXCollections.observableArrayList(allUserBookings));
            }
        } catch (Exception e) {
            LoggerUtil.warning("Unable to fetch fresh live dashboard data from MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleQuickCheckIn() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (nextActiveBooking == null || user == null) return;
        try {
            bookingService.checkIn(nextActiveBooking.getId(), user.getId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Checked in successfully! Enjoy your study session.", ButtonType.OK);
            alert.setHeaderText("Check-In Confirmed");
            alert.showAndWait();
            loadDashboardData();
        } catch (InvalidBookingException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alert.setHeaderText("Check-In Notice");
            alert.showAndWait();
        }
    }

    @FXML
    public void handleQuickBookSeat() {
        // Find MainController to trigger view switch
        if (lblTotalSeats.getScene() != null && lblTotalSeats.getScene().lookup("#btnNavSeatMap") instanceof Button btn) {
            btn.fire();
        }
    }

    @FXML
    public void handleViewAllBookings() {
        if (lblTotalSeats.getScene() != null && lblTotalSeats.getScene().lookup("#btnNavMyBookings") instanceof Button btn) {
            btn.fire();
        }
    }
}
