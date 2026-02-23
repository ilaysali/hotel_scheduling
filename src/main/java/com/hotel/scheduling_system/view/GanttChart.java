package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class GanttChart extends VBox {
    private final Pane drawingPane;

    public GanttChart() {
        setSpacing(10);
        Label title = new Label("Room Scheduling Calendar");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");

        drawingPane = new Pane();
        drawingPane.setMinSize(500, 300);

        ScrollPane scrollPane = new ScrollPane(drawingPane);
        scrollPane.setPrefSize(500, 300);
        scrollPane.setStyle("-fx-background-color: transparent;");

        getChildren().addAll(title, scrollPane);
    }

    public void updateData(Map<Room, List<Reservation>> assignments) {
        drawingPane.getChildren().clear();
        if (assignments.isEmpty()) return;

        // Find the earliest date across ALL reservations
        LocalDate earliestDate = assignments.values().stream()
                .flatMap(List::stream)
                .map(Reservation::startDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        int yOffset = 20;
        int dayWidth = 40;
        int barHeight = 30;
        int roomLabelWidth = 100; // Space reserved on the left for room names

        // Loop through every room (The Y-Axis)
        for (Map.Entry<Room, List<Reservation>> entry : assignments.entrySet()) {
            Room room = entry.getKey();
            List<Reservation> reservations = entry.getValue();

            // 1. Draw Room Label on the left
            Label roomLabel = new Label("Room " + room.getRoomNumber() + "\n(" + room.getType() + ")");
            roomLabel.setLayoutX(10);
            roomLabel.setLayoutY(yOffset);
            roomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            drawingPane.getChildren().add(roomLabel);

            // 2. Draw horizontal guide line for the lane
            Line guideLine = new Line(roomLabelWidth, yOffset + barHeight + 5, 800, yOffset + barHeight + 5);
            guideLine.setStroke(Color.LIGHTGRAY);
            drawingPane.getChildren().add(guideLine);

            // 3. Draw each reservation inside this room's lane (The X-Axis)
            for (Reservation res : reservations) {
                long startDayOffset = ChronoUnit.DAYS.between(earliestDate, res.startDate());
                long duration = ChronoUnit.DAYS.between(res.startDate(), res.endDate());

                double xPos = roomLabelWidth + (startDayOffset * dayWidth);
                double width = duration * dayWidth;

                Rectangle bar = new Rectangle(xPos, yOffset, width, barHeight);
                bar.setFill(res.roomType().name().equals("DOUBLE") ? Color.LIGHTBLUE : Color.LIGHTGREEN);
                bar.setStroke(Color.BLACK);
                bar.setArcWidth(10);
                bar.setArcHeight(10);

                Label resLabel = new Label("Res " + res.id());
                resLabel.setLayoutX(xPos + 5);
                resLabel.setLayoutY(yOffset + 5);
                resLabel.setStyle("-fx-font-size: 11px;");

                drawingPane.getChildren().addAll(bar, resLabel);
            }

            // Move down 50 pixels for the next room lane!
            yOffset += 50;
        }
    }
}