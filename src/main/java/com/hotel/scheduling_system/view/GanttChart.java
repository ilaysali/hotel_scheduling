package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GanttChart extends VBox {
    private final Pane drawingPane;

    @Setter
    private Consumer<Reservation> onRejectCallback;

    @Setter
    private Consumer<Reservation> onApproveCallback;

    public GanttChart() {
        setSpacing(10);
        Label title = new Label("Room Scheduling Calendar");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: green;");

        drawingPane = new Pane();
        drawingPane.setMinSize(700, 400);

        ScrollPane scrollPane = new ScrollPane(drawingPane);
        scrollPane.setPrefSize(700, 400);
        scrollPane.setStyle("-fx-background-color: transparent;");

        getChildren().addAll(title, scrollPane);
    }

    // The function now also receives the list of approved downgrades, and returns whether there are still pending ones
    public boolean updateData(Map<Room, List<Reservation>> assignments, Set<Reservation> approvedDowngrades) {
        drawingPane.getChildren().clear();
        if (assignments == null || assignments.isEmpty()) return false;

        boolean hasPendingDowngrades = false;
        LocalDate earliestDate = null;
        LocalDate latestDate = null;

        for (List<Reservation> resList : assignments.values()) {
            for (Reservation res : resList) {
                if (earliestDate == null || res.startDate().isBefore(earliestDate)) earliestDate = res.startDate();
                if (latestDate == null || res.endDate().isAfter(latestDate)) latestDate = res.endDate();
            }
        }

        if (earliestDate == null) return false;

        int dayWidth = 50;
        int barHeight = 30;
        int roomLabelWidth = 100;
        long totalDays = ChronoUnit.DAYS.between(earliestDate, latestDate) + 2;

        int rulerY = 30;
        Line rulerLine = new Line(roomLabelWidth, rulerY, roomLabelWidth + (totalDays * dayWidth), rulerY);
        rulerLine.setStroke(Color.DARKGRAY);
        drawingPane.getChildren().add(rulerLine);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 0; i <= totalDays; i++) {
            double xPos = roomLabelWidth + (i * dayWidth);
            Line tick = new Line(xPos, rulerY - 5, xPos, rulerY + 5);
            tick.setStroke(Color.DARKGRAY);

            Label dateLabel = new Label(earliestDate.plusDays(i).format(formatter));
            dateLabel.setLayoutX(xPos - 15);
            dateLabel.setLayoutY(rulerY - 20);
            dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: dimgray;");

            Line gridLine = new Line(xPos, rulerY, xPos, 800);
            gridLine.setStroke(Color.LIGHTGRAY);
            gridLine.getStrokeDashArray().addAll(5d, 5d);
            drawingPane.getChildren().addAll(gridLine, tick, dateLabel);
        }

        int yOffset = 70;

        for (Map.Entry<Room, List<Reservation>> entry : assignments.entrySet()) {
            Room room = entry.getKey();
            List<Reservation> reservations = entry.getValue();

            Label roomLabel = new Label("Room " + room.roomNumber() + "\n(" + room.type() + ")");
            roomLabel.setLayoutX(10);
            roomLabel.setLayoutY(yOffset);
            roomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            drawingPane.getChildren().add(roomLabel);

            Line guideLine = new Line(roomLabelWidth, yOffset + barHeight + 10, roomLabelWidth + (totalDays * dayWidth), yOffset + barHeight + 10);
            guideLine.setStroke(Color.LIGHTGRAY);
            drawingPane.getChildren().add(guideLine);

            for (Reservation res : reservations) {
                long startDayOffset = ChronoUnit.DAYS.between(earliestDate, res.startDate());
                long duration = ChronoUnit.DAYS.between(res.startDate(), res.endDate());
                double xPos = roomLabelWidth + (startDayOffset * dayWidth);
                double width = duration * dayWidth;

                Color barColor = switch (res.roomType().name()) {
                    case "SUITE" -> Color.GOLD;
                    case "DOUBLE" -> Color.LIGHTBLUE;
                    default -> Color.LIGHTGREEN;
                };

                Rectangle bar = new Rectangle(xPos, yOffset, width, barHeight);
                bar.setFill(barColor);
                bar.setArcWidth(10);
                bar.setArcHeight(10);

                // Utilizing the RoomType enum getRank() directly to replace the old switch statement!
                int requestedRank = res.roomType().getRank();
                int assignedRank = room.type().getRank();

                boolean isDowngrade = assignedRank < requestedRank;
                boolean isPendingDowngrade = isDowngrade && !approvedDowngrades.contains(res);
                boolean isUpgrade = assignedRank > requestedRank;

                // Smart coloring based on status
                if (isPendingDowngrade) {
                    hasPendingDowngrades = true;
                    bar.setStroke(Color.RED);
                    bar.setStrokeWidth(2.5);
                    bar.getStrokeDashArray().addAll(4d, 4d); // Dashed red (requires attention!)
                } else if (isDowngrade) {
                    bar.setStroke(Color.ORANGE);
                    bar.setStrokeWidth(2); // Solid orange (means the manager approved the downgrade)
                } else if (isUpgrade) {
                    bar.setStroke(Color.BLUE);
                    bar.setStrokeWidth(1.5);
                    bar.getStrokeDashArray().addAll(2d, 2d); // Dashed blue for upgrade (for info only, doesn't require approval)
                } else {
                    bar.setStroke(Color.BLACK);
                    bar.setStrokeWidth(1);
                }

                Label resLabel = new Label("Res " + res.id());
                resLabel.setLayoutX(xPos + 5);
                resLabel.setLayoutY(yOffset + 5);
                resLabel.setStyle("-fx-font-size: 11px;");

                String tooltipText = "Guest Name: " + res.guestName() + "\n" +
                        "Dates: " + res.startDate() + " to " + res.endDate() + "\n" +
                        "Requested: " + res.roomType();

                if (isDowngrade) {
                    String status = isPendingDowngrade ? "(PENDING APPROVAL)" : "(APPROVED)";
                    tooltipText += "\n\n⚠️ ALERT: Downgrade to " + room.type() + "! " + status;
                } else if (isUpgrade) {
                    tooltipText += "\n\n✨ INFO: Upgrade to " + room.type();
                }

                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(0,0,0,0.8);");
                tooltip.setShowDelay(Duration.millis(100));

                Tooltip.install(bar, tooltip);
                Tooltip.install(resLabel, tooltip);

                ContextMenu contextMenu = new ContextMenu();

                // Adding the approved option (only if it's a downgrade that hasn't been approved yet)
                if (isPendingDowngrade) {
                    MenuItem approveItem = new MenuItem("Approve Downgrade");
                    approveItem.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    approveItem.setOnAction(e -> {
                        if (onApproveCallback != null) onApproveCallback.accept(res);
                    });
                    contextMenu.getItems().add(approveItem);
                }

                MenuItem rejectItem = new MenuItem("Reject Assignment (Move to Unassigned)");
                rejectItem.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                rejectItem.setOnAction(e -> {
                    if (onRejectCallback != null) onRejectCallback.accept(res);
                });

                contextMenu.getItems().add(rejectItem);

                bar.setOnContextMenuRequested(e -> contextMenu.show(bar, e.getScreenX(), e.getScreenY()));
                resLabel.setOnContextMenuRequested(e -> contextMenu.show(resLabel, e.getScreenX(), e.getScreenY()));

                drawingPane.getChildren().addAll(bar, resLabel);
            }
            yOffset += 60;
        }

        return hasPendingDowngrades;
    }
}