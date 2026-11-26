package com.library.seatmgmt.controller;

import com.library.seatmgmt.dao.BookingDAO;
import com.library.seatmgmt.dao.impl.BookingDAOImpl;
import com.library.seatmgmt.exception.InvalidBookingException;
import com.library.seatmgmt.model.Booking;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.model.enums.BookingStatus;
import com.library.seatmgmt.service.BookingService;
import com.library.seatmgmt.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for managing student bookings, check-ins, check-outs, and cancellations.
 */
public class MyBookingsController {

    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TableView<Booking> tblBookings;
    @FXML private TableColumn<Booking, String> colId;
    @FXML private TableColumn<Booking, String> colSeat;
    @FXML private TableColumn<Booking, String> colZone;
    @FXML private TableColumn<Booking, String> colDate;
    @FXML private TableColumn<Booking, String> colTime;
    @FXML private TableColumn<Booking, String> colStatus;
    @FXML private TableColumn<Booking, String> colCheckIn;

    @FXML private Button btnCheckIn;
    @FXML private Button btnCheckOut;
    @FXML private Button btnCancel;

    private final BookingDAO bookingDAO = new BookingDAOImpl();
    private final BookingService bookingService = new BookingService();

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US);

    @FXML
    public void initialize() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList("ALL", "RESERVED", "OCCUPIED", "COMPLETED", "CANCELLED", "NO_SHOW"));
        cmbStatusFilter.setValue("ALL");
        cmbStatusFilter.setOnAction(e -> filterBookings());

        setupTableColumns();
        tblBookings.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateActionButtons(newSel));

        loadBookings();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colSeat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeatNumber()));
        colZone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getZoneName()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingDate().toString()));
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTimeSlot()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
        colCheckIn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCheckInTime() != null ? d.getValue().getCheckInTime().format(TS_FMT) : "--"
        ));

        // Custom status badge
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
    public void loadBookings() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        List<Booking> list = bookingDAO.findByUserId(user.getId());
        tblBookings.setItems(FXCollections.observableArrayList(list));
        filterBookings();
    }

    private void filterBookings() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        String selectedFilter = cmbStatusFilter.getValue();
        List<Booking> all = bookingDAO.findByUserId(user.getId());

        if ("ALL".equalsIgnoreCase(selectedFilter)) {
            tblBookings.setItems(FXCollections.observableArrayList(all));
        } else {
            List<Booking> filtered = all.stream()
                    .filter(b -> b.getStatus().name().equalsIgnoreCase(selectedFilter))
                    .toList();
            tblBookings.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    private void updateActionButtons(Booking b) {
        if (b == null) {
            btnCheckIn.setDisable(true);
            btnCheckOut.setDisable(true);
            btnCancel.setDisable(true);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        btnCheckIn.setDisable(!b.isEligibleForCheckIn(now));
        btnCheckOut.setDisable(b.getStatus() != BookingStatus.OCCUPIED);
        btnCancel.setDisable(b.getStatus() != BookingStatus.RESERVED);
    }

    @FXML
    public void handleCheckIn() {
        Booking sel = tblBookings.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User user = SessionContext.getInstance().getCurrentUser();

        try {
            bookingService.checkIn(sel.getId(), user.getId());
            showAlert(Alert.AlertType.INFORMATION, "Checked In", "You have successfully checked in to Seat " + sel.getSeatNumber());
            loadBookings();
        } catch (InvalidBookingException e) {
            showAlert(Alert.AlertType.WARNING, "Check-In Notice", e.getMessage());
        }
    }

    @FXML
    public void handleCheckOut() {
        Booking sel = tblBookings.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User user = SessionContext.getInstance().getCurrentUser();

        try {
            bookingService.checkOut(sel.getId(), user.getId());
            showAlert(Alert.AlertType.INFORMATION, "Checked Out", "Seat released. Thank you for using the library!");
            loadBookings();
        } catch (InvalidBookingException e) {
            showAlert(Alert.AlertType.WARNING, "Notice", e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        Booking sel = tblBookings.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to cancel booking #" + sel.getId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Cancellation");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            User user = SessionContext.getInstance().getCurrentUser();
            try {
                bookingService.cancelBooking(sel.getId(), user.getId());
                showAlert(Alert.AlertType.INFORMATION, "Cancelled", "Booking has been cancelled.");
                loadBookings();
            } catch (InvalidBookingException e) {
                showAlert(Alert.AlertType.WARNING, "Notice", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
