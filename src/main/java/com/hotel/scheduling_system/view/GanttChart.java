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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class GanttChart extends VBox {
    private final Pane drawingPane;

    public GanttChart() {
        setSpacing(10);
        Label title = new Label("Room Scheduling Calendar");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: green;");

        drawingPane = new Pane();
        drawingPane.setMinSize(700, 400); // הגדלנו קצת את שטח הציור כדי שיהיה מקום

        ScrollPane scrollPane = new ScrollPane(drawingPane);
        scrollPane.setPrefSize(700, 400);
        scrollPane.setStyle("-fx-background-color: transparent;");

        getChildren().addAll(title, scrollPane);
    }

    public void updateData(Map<Room, List<Reservation>> assignments) {
        drawingPane.getChildren().clear();
        if (assignments == null || assignments.isEmpty()) return;

        // 1. חיפוש התאריך המוקדם והמאוחר ביותר כדי לדעת כמה ארוך לצייר את הסרגל
        LocalDate earliestDate = null;
        LocalDate latestDate = null;

        for (List<Reservation> resList : assignments.values()) {
            for (Reservation res : resList) {
                if (earliestDate == null || res.startDate().isBefore(earliestDate)) {
                    earliestDate = res.startDate();
                }
                if (latestDate == null || res.endDate().isAfter(latestDate)) {
                    latestDate = res.endDate();
                }
            }
        }

        if (earliestDate == null) return;

        int dayWidth = 50;  // רוחב כל יום בפיקסלים (הגדלנו כדי שהטקסט ייכנס בנוחות)
        int barHeight = 30; // גובה כרטיסיית ההזמנה
        int roomLabelWidth = 100; // השטח השמור בצד שמאל לשמות החדרים

        long totalDays = ChronoUnit.DAYS.between(earliestDate, latestDate) + 2; // +2 ימים כרווח ביטחון

        // --- ציור סרגל התאריכים (ציר ה-X למעלה) ---
        int rulerY = 30; // המיקום האנכי של הסרגל
        Line rulerLine = new Line(roomLabelWidth, rulerY, roomLabelWidth + (totalDays * dayWidth), rulerY);
        rulerLine.setStroke(Color.DARKGRAY);
        drawingPane.getChildren().add(rulerLine);

        // פורמט תאריך קצר, למשל: Mar 01
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 0; i <= totalDays; i++) {
            double xPos = roomLabelWidth + (i * dayWidth);

            // שנתה (Tick mark) קטנה על הסרגל
            Line tick = new Line(xPos, rulerY - 5, xPos, rulerY + 5);
            tick.setStroke(Color.DARKGRAY);

            // טקסט התאריך
            Label dateLabel = new Label(earliestDate.plusDays(i).format(formatter));
            dateLabel.setLayoutX(xPos - 15);
            dateLabel.setLayoutY(rulerY - 20);
            dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: dimgray;");

            // קו רשת מקווקו שיורד למטה כדי להקל על הקריאה!
            Line gridLine = new Line(xPos, rulerY, xPos, 800);
            gridLine.setStroke(Color.LIGHTGRAY);
            gridLine.getStrokeDashArray().addAll(5d, 5d); // עושה את הקו מקווקו

            drawingPane.getChildren().addAll(gridLine, tick, dateLabel);
        }

        // --- ציור החדרים וההזמנות עצמן ---
        int yOffset = 70; // מתחילים לצייר את החדרים קצת יותר למטה, מתחת לסרגל התאריכים

        for (Map.Entry<Room, List<Reservation>> entry : assignments.entrySet()) {
            Room room = entry.getKey();
            List<Reservation> reservations = entry.getValue();

            // ציור שם החדר בצד שמאל
            Label roomLabel = new Label("Room " + room.getRoomNumber() + "\n(" + room.getType() + ")");
            roomLabel.setLayoutX(10);
            roomLabel.setLayoutY(yOffset);
            roomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            drawingPane.getChildren().add(roomLabel);

            // קו הפרדה אופקי בין חדר לחדר (Swimlane)
            Line guideLine = new Line(roomLabelWidth, yOffset + barHeight + 10, roomLabelWidth + (totalDays * dayWidth), yOffset + barHeight + 10);
            guideLine.setStroke(Color.LIGHTGRAY);
            drawingPane.getChildren().add(guideLine);

            // ציור ההזמנות בתוך הנתיב של החדר
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

            // יורדים למטה כדי לצייר את נתיב החדר הבא
            yOffset += 60;
        }
    }
}