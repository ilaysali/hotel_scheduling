package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.scene.Node;
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
import java.util.ArrayList;
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

    // Layout Constants
    private static final int DAY_WIDTH = 50;
    private static final int BAR_HEIGHT = 30;
    private static final int ROOM_LABEL_WIDTH = 100;
    private static final int ROW_HEIGHT = 60;
    private static final int INITIAL_Y_OFFSET = 70;
    private static final int RULER_Y = 30;

    public GanttChart() {
        setSpacing(10);
        Label title = new Label("Room Scheduling Calendar");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: green;");

        drawingPane = new Pane();
        drawingPane.setCache(true);
        drawingPane.setCacheHint(javafx.scene.CacheHint.SPEED);

        ScrollPane scrollPane = new ScrollPane(drawingPane);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        this.setMinWidth(350);
        getChildren().addAll(title, scrollPane);
    }

    /**
     * Main update function that orchestrates the drawing of the Gantt chart.
     */
    public boolean updateData(Map<Room, List<Reservation>> assignments, Set<Reservation> approvedDowngrades) {
        drawingPane.getChildren().clear();
        if (assignments == null || assignments.isEmpty()) return false;

        LocalDate[] dateRange = calculateDateRange(assignments);
        LocalDate earliestDate = dateRange[0];
        LocalDate latestDate = dateRange[1];
        if (earliestDate == null) return false;

        long totalDays = ChronoUnit.DAYS.between(earliestDate, latestDate) + 2;
        List<Node> nodesToAdd = new ArrayList<>();

        setupCanvasDimensions(totalDays, assignments.size());
        drawTimeline(nodesToAdd, earliestDate, totalDays);

        boolean hasPendingDowngrades = false;
        int yOffset = INITIAL_Y_OFFSET;

        for (Map.Entry<Room, List<Reservation>> entry : assignments.entrySet()) {
            hasPendingDowngrades |= drawRoomRow(nodesToAdd, entry.getKey(), entry.getValue(),
                    earliestDate, approvedDowngrades, totalDays, yOffset);
            yOffset += ROW_HEIGHT;
        }

        drawingPane.getChildren().addAll(nodesToAdd);
        return hasPendingDowngrades;
    }

    /**
     * Finds the earliest start date and latest end date among all assignments.
     * Returns an array where index 0 is earliestDate, and index 1 is latestDate.
     */
    private LocalDate[] calculateDateRange(Map<Room, List<Reservation>> assignments) {
        LocalDate earliestDate = null;
        LocalDate latestDate = null;

        for (List<Reservation> resList : assignments.values()) {
            for (Reservation res : resList) {
                if (earliestDate == null || res.startDate().isBefore(earliestDate)) earliestDate = res.startDate();
                if (latestDate == null || res.endDate().isAfter(latestDate)) latestDate = res.endDate();
            }
        }
        return new LocalDate[]{earliestDate, latestDate};
    }

    /**
     * Sets the width and height of the drawing pane based on the data span.
     */
    private void setupCanvasDimensions(long totalDays, int numRooms) {
        double totalWidth = ROOM_LABEL_WIDTH + (totalDays * DAY_WIDTH) + 50;
        double totalHeight = INITIAL_Y_OFFSET + (numRooms * ROW_HEIGHT) + 20;

        drawingPane.setPrefSize(totalWidth, totalHeight);
        drawingPane.setMinSize(totalWidth, totalHeight);
    }

    /**
     * Draws the top ruler with dates and the vertical grid lines.
     */
    private void drawTimeline(List<Node> nodesToAdd, LocalDate earliestDate, long totalDays) {
        Line rulerLine = new Line(ROOM_LABEL_WIDTH, RULER_Y, ROOM_LABEL_WIDTH + (totalDays * DAY_WIDTH), RULER_Y);
        rulerLine.setStroke(Color.DARKGRAY);
        nodesToAdd.add(rulerLine);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        double totalHeight = drawingPane.getPrefHeight();

        for (int i = 0; i <= totalDays; i++) {
            double xPos = ROOM_LABEL_WIDTH + (i * DAY_WIDTH);

            Line tick = new Line(xPos, RULER_Y - 5, xPos, RULER_Y + 5);
            tick.setStroke(Color.DARKGRAY);

            Label dateLabel = new Label(earliestDate.plusDays(i).format(formatter));
            dateLabel.setLayoutX(xPos - 15);
            dateLabel.setLayoutY(RULER_Y - 20);
            dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: dimgray;");

            Line gridLine = new Line(xPos, RULER_Y, xPos, totalHeight);
            gridLine.setStroke(Color.rgb(235, 235, 235));

            nodesToAdd.add(gridLine);
            nodesToAdd.add(tick);
            nodesToAdd.add(dateLabel);
        }
    }

    /**
     * Draws a single room row including its label, guideline, and all its scheduled reservations.
     */
    private boolean drawRoomRow(List<Node> nodesToAdd, Room room, List<Reservation> reservations,
                                LocalDate earliestDate, Set<Reservation> approvedDowngrades,
                                long totalDays, int yOffset) {

        Label roomLabel = new Label("Room " + room.roomNumber() + "\n(" + room.type() + ")\n[" + room.view() + "]");
        roomLabel.setLayoutX(10);
        roomLabel.setLayoutY(yOffset - 10);
        roomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        nodesToAdd.add(roomLabel);

        Line guideLine = new Line(ROOM_LABEL_WIDTH, yOffset + BAR_HEIGHT + 10,
                ROOM_LABEL_WIDTH + (totalDays * DAY_WIDTH), yOffset + BAR_HEIGHT + 10);
        guideLine.setStroke(Color.LIGHTGRAY);
        nodesToAdd.add(guideLine);

        boolean hasPendingDowngrades = false;

        for (Reservation res : reservations) {
            hasPendingDowngrades |= createReservationBar(nodesToAdd, res, room, earliestDate,
                    approvedDowngrades, yOffset);
        }
        return hasPendingDowngrades;
    }

    /**
     * Creates and configures the visual elements (Rectangle, Label, Tooltip, ContextMenu) for a single reservation.
     */
    private boolean createReservationBar(List<Node> nodesToAdd, Reservation res, Room room,
                                         LocalDate earliestDate, Set<Reservation> approvedDowngrades, int yOffset) {

        long startDayOffset = ChronoUnit.DAYS.between(earliestDate, res.startDate());
        long duration = ChronoUnit.DAYS.between(res.startDate(), res.endDate());
        double xPos = ROOM_LABEL_WIDTH + (startDayOffset * DAY_WIDTH);
        double width = duration * DAY_WIDTH;

        Rectangle bar = new Rectangle(xPos, yOffset, width, BAR_HEIGHT);
        bar.setFill(Color.web(res.roomType().getColorName()));
        bar.setArcWidth(10);
        bar.setArcHeight(10);

        int requestedRank = res.roomType().getRank();
        int assignedRank = room.type().getRank();
        boolean isDowngrade = assignedRank < requestedRank;
        boolean isPendingDowngrade = isDowngrade && !approvedDowngrades.contains(res);
        boolean isUpgrade = assignedRank > requestedRank;
        boolean isViewMismatch = res.preferredView() != null && !res.preferredView().equals(room.view());

        applyStyling(bar, isPendingDowngrade, isDowngrade, isUpgrade, isViewMismatch);

        Label resLabel = new Label("Res " + res.id());
        resLabel.setLayoutX(xPos + 5);
        resLabel.setLayoutY(yOffset + 5);
        resLabel.setStyle("-fx-font-size: 11px;");

        setupInteractivity(bar, resLabel, res, room, isPendingDowngrade, isDowngrade, isUpgrade, isViewMismatch);

        nodesToAdd.add(bar);
        nodesToAdd.add(resLabel);

        return isPendingDowngrade;
    }

    /**
     * Applies styling to the reservation bar based on its assignment status.
     */
    private void applyStyling(Rectangle bar, boolean isPendingDowngrade, boolean isDowngrade,
                              boolean isUpgrade, boolean isViewMismatch) {
        if (isPendingDowngrade) {
            bar.setStroke(Color.RED);
            bar.setStrokeWidth(2.5);
            bar.getStrokeDashArray().addAll(4d, 4d);
        } else if (isDowngrade) {
            bar.setStroke(Color.ORANGE);
            bar.setStrokeWidth(2);
        } else if (isUpgrade) {
            bar.setStroke(Color.BLUE);
            bar.setStrokeWidth(1.5);
            bar.getStrokeDashArray().addAll(2d, 2d);
        } else {
            bar.setStroke(Color.BLACK);
            bar.setStrokeWidth(1);
        }

        if (isViewMismatch) {
            bar.setEffect(new javafx.scene.effect.InnerShadow(10, Color.DARKRED));
            bar.setCache(true);
        }
    }

    /**
     * Sets up Tooltips and ContextMenus for user interaction with the reservation.
     */
    private void setupInteractivity(Rectangle bar, Label resLabel, Reservation res, Room room,
                                    boolean isPendingDowngrade, boolean isDowngrade,
                                    boolean isUpgrade, boolean isViewMismatch) {

        // Tooltip Setup
        String viewRequestedText = (res.preferredView() != null) ? res.preferredView() : "Any";
        String tooltipText = "Guest Name: " + res.guestName() + "\n" +
                "Dates: " + res.startDate() + " to " + res.endDate() + "\n" +
                "Requested Room: " + res.roomType() + "\n" +
                "Requested View: " + viewRequestedText;

        if (isDowngrade) {
            String status = isPendingDowngrade ? "(PENDING APPROVAL)" : "(APPROVED)";
            tooltipText += "\n\n⚠️ ALERT: Downgrade to " + room.type() + "! " + status;
        } else if (isUpgrade) {
            tooltipText += "\n\n✨ INFO: Upgrade to " + room.type();
        }

        if (isViewMismatch) {
            tooltipText += "\n\n   INFO: Assigned view (" + room.view() + ") differs from requested view.";
        }

        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(0,0,0,0.8);");
        tooltip.setShowDelay(Duration.millis(100));
        Tooltip.install(bar, tooltip);
        Tooltip.install(resLabel, tooltip);

        // Context Menu Setup
        javafx.event.EventHandler<javafx.scene.input.ContextMenuEvent> contextMenuHandler = e -> {
            ContextMenu contextMenu = new ContextMenu();

            if (isPendingDowngrade) {
                MenuItem approveItem = new MenuItem("Approve Downgrade");
                approveItem.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                approveItem.setOnAction(ev -> {
                    if (onApproveCallback != null) onApproveCallback.accept(res);
                });
                contextMenu.getItems().add(approveItem);
            }

            MenuItem rejectItem = new MenuItem("Reject Assignment (Move to Unassigned)");
            rejectItem.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            rejectItem.setOnAction(ev -> {
                if (onRejectCallback != null) onRejectCallback.accept(res);
            });
            contextMenu.getItems().add(rejectItem);

            contextMenu.show(bar, e.getScreenX(), e.getScreenY());
        };

        bar.setOnContextMenuRequested(contextMenuHandler);
        resLabel.setOnContextMenuRequested(contextMenuHandler);
    }
}