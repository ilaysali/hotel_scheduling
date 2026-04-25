package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.model.Reservation;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;

public class UnassignedPanel extends VBox {
    private final ListView<String> listView;

    public UnassignedPanel() {
        setSpacing(10);
        Label title = new Label("Unassigned (Conflicts)");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");

        listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS); // Allow the list to stretch vertically to fill the screen
        getChildren().addAll(title, listView);
    }

    public void updateData(List<Reservation> unassigned) {
        listView.getItems().clear();
        for (Reservation r : unassigned) {
            listView.getItems().add("Res ID: " + r.id() + " | " + r.startDate() + " to " + r.endDate());
        }
    }
}