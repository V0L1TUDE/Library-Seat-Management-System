package com.library.seatmgmt;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FxmlLoadTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    @Test
    void testLoadAllFxmls() throws Exception {
        List<String> fxmlFiles = List.of(
                "/fxml/login.fxml",
                "/fxml/main_layout.fxml",
                "/fxml/student_dashboard.fxml",
                "/fxml/seat_map.fxml",
                "/fxml/my_bookings.fxml",
                "/fxml/admin_dashboard.fxml",
                "/fxml/reports.fxml"
        );

        for (String file : fxmlFiles) {
            System.out.println("Testing FXML: " + file);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(file));
                Object root = loader.load();
                assertNotNull(root, "Root node must not be null for " + file);
                System.out.println("PASS: " + file);
            } catch (Exception e) {
                System.err.println("FAILED FXML: " + file);
                e.printStackTrace();
                throw e;
            }
        }
    }
}
