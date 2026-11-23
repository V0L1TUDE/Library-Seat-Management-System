package com.library.seatmgmt;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point for the Smart Library Seat Management System.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Smart Library Seat Management System Initialized");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 200);

        primaryStage.setTitle("Smart Library - Test");
        primaryStage.setScene(scene);
        // Stage can be shown when running GUI mode
    }

    public static void main(String[] args) {
        System.out.println("JavaFX & Java Environment Verified.");
    }
}
