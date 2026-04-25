package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.model.Reservation;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;

public class AllReservationsPanel extends VBox {
    private final ListView<String> listView;

    public AllReservationsPanel() {
        setSpacing(10);


        // Setup title label
        Label title = new Label("All Reservations");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;"); // Blue color for distinction

        // Initialize ListView
        listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS); // Allow the list to stretch vertically to fill the screen

        getChildren().addAll(title, listView);
    }

    public void updateData(List<Reservation> allReservations) {
        listView.getItems().clear();
        for (Reservation r : allReservations) {
            // Format the string to show more details using the Reservation record
            String displayText = String.format("ID: %d | %s | %s\n%s to %s",
                    r.id(), r.guestName(), r.roomType(), r.startDate(), r.endDate());
            listView.getItems().add(displayText);
        }
    }
}