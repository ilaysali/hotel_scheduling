package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MainView extends VBox {

    private final AppController appController;
    private final GanttChart ganttChart;
    private final UnassignedPanel unassignedPanel;
    private final Button saveBtn;

    private Map<Room, List<Reservation>> currentAssignments;
    private List<Reservation> currentUnassigned;
    private final Set<Reservation> approvedDowngrades = new HashSet<>();

    public MainView(AppController appController) {
        this.appController = appController;
        this.ganttChart = new GanttChart();
        this.unassignedPanel = new UnassignedPanel();

        setPadding(new Insets(20));
        setSpacing(15);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Hotel Scheduling System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fitnessLabel = new Label("AI Fitness Score: Waiting...");
        fitnessLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #673AB7; -fx-background-color: #EDE7F6; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");

        headerBox.getChildren().addAll(titleLabel, spacer, fitnessLabel);

        Button mockBtn = new Button("1. Reset & Load Mock Data");
        mockBtn.setStyle("-fx-base: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");

        Button generateBtn = new Button("2. Generate Schedule");

        saveBtn = new Button("3. Save Schedule to Database");
        saveBtn.setDisable(true);
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        // --- הכפתור החדש ליצירת הזמנה ידנית! ---
        Button addResBtn = new Button("➕ New Reservation");
        addResBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonsBox = new HBox(15, mockBtn, generateBtn, saveBtn, addResBtn);
        HBox dashboard = new HBox(20, ganttChart, unassignedPanel);

        // --- לוגיקת החלון הקופץ (Dialog) ---
        addResBtn.setOnAction(e -> openNewReservationDialog());

        ganttChart.setOnRejectCallback(rejectedRes -> {
            if (currentAssignments != null && currentUnassigned != null) {
                for (List<Reservation> roomSchedule : currentAssignments.values()) {
                    if (roomSchedule.remove(rejectedRes)) break;
                }
                currentUnassigned.add(rejectedRes);
                approvedDowngrades.remove(rejectedRes);
                refreshDashboard();
            }
        });

        ganttChart.setOnApproveCallback(approvedRes -> {
            approvedDowngrades.add(approvedRes);
            refreshDashboard();
        });

        mockBtn.setOnAction(event -> {
            appController.onLoadMockDataClicked();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Database Reset");
            alert.setHeaderText(null);
            alert.setContentText("Mock data loaded successfully! You can now generate the schedule.");
            alert.showAndWait();

            fitnessLabel.setText("AI Fitness Score: Waiting...");
            approvedDowngrades.clear();
            saveBtn.setDisable(true);
        });

        generateBtn.setOnAction(event -> {
            try {
                Map<String, Object> results = appController.onGenerateScheduleClicked();
                currentAssignments = (Map<Room, List<Reservation>>) results.get("ASSIGNMENTS");
                currentUnassigned = (List<Reservation>) results.get("UNASSIGNED");
                Double fitnessScore = (Double) results.get("FITNESS_SCORE");

                approvedDowngrades.clear();
                refreshDashboard();

                if (fitnessScore != null) fitnessLabel.setText(String.format("AI Fitness Score: %,.0f", fitnessScore));
            } catch (IllegalArgumentException e) {
                // תופסים את החריגה הספציפית שיצרנו ומדפיסים אותה למסך!
                System.err.println("Algorithm halted: " + e.getMessage());

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Data Error");
                errorAlert.setHeaderText("Algorithm Halted");
                errorAlert.setContentText(e.getMessage());
                errorAlert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveBtn.setOnAction(event -> {
            if (currentAssignments != null) {
                appController.onSaveScheduleClicked(currentAssignments);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Schedule saved successfully to the database!");
                alert.showAndWait();
            }
        });

        getChildren().addAll(headerBox, buttonsBox, dashboard);
    }

    private void refreshDashboard() {
        if (currentAssignments == null) return;
        boolean hasPendingDowngrades = ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);
        saveBtn.setDisable(currentAssignments.isEmpty() || hasPendingDowngrades);
    }

    // הפונקציה שבונה ומציגה את טופס ההזמנה החדשה
    private void openNewReservationDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Reservation");
        dialog.setHeaderText("Please fill out the reservation details:");

        // הפיכת תיבת האורחים לתיבת טקסט משולבת (Editable ComboBox)
        ComboBox<String> guestComboBox = new ComboBox<>();
        for (Guest g : appController.getAllGuests()) {
            guestComboBox.getItems().add(g.firstName() + " " + g.lastName());
        }
        guestComboBox.setEditable(true); // עכשיו המנהל יכול להקליד כל שם שירצה!
        guestComboBox.setPromptText("Select or type a name...");

        DatePicker startDatePicker = new DatePicker(LocalDate.of(2026, 4, 10));
        DatePicker endDatePicker = new DatePicker(LocalDate.of(2026, 4, 15));

        ComboBox<String> roomTypeBox = new ComboBox<>();
        roomTypeBox.getItems().addAll("SINGLE", "DOUBLE", "SUITE");
        roomTypeBox.setPromptText("Select Room Type...");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Guest Name:"), 0, 0); grid.add(guestComboBox, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1); grid.add(startDatePicker, 1, 1);
        grid.add(new Label("End Date:"), 0, 2); grid.add(endDatePicker, 1, 2);
        grid.add(new Label("Requested Room:"), 0, 3); grid.add(roomTypeBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // === ולידציה שמונעת מהחלון להיסגר במקרה של שגיאה ===
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String guestName = guestComboBox.getValue();
            String roomType = roomTypeBox.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            // 1. חסר שם או סוג חדר
            if (guestName == null || guestName.trim().isEmpty() || roomType == null) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Missing Information");
                errorAlert.setHeaderText("Validation Failed");
                errorAlert.setContentText("You must provide a guest name and select a room type.");
                errorAlert.showAndWait();
                event.consume(); // מונע מהחלון להיסגר!
                return;
            }

            // 2. תאריכים שגויים
            if (start == null || end == null || !end.isAfter(start)) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Invalid Dates");
                errorAlert.setHeaderText("Validation Failed");
                errorAlert.setContentText("The End Date must be strictly after the Start Date.");
                errorAlert.showAndWait();
                event.consume(); // מונע מהחלון להיסגר!
                return;
            }
        });

        // אם הכל עבר תקין, מבצעים את השמירה בפועל
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // שולחים את השם לבדיקה - אם הוא חדש הוא יתווסף אוטומטית למסד הנתונים!
                int guestId = appController.getOrCreateGuest(guestComboBox.getValue());

                appController.createNewReservation(
                        guestId,
                        roomTypeBox.getValue(),
                        startDatePicker.getValue(),
                        endDatePicker.getValue()
                );

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reservation Added");
                alert.setHeaderText(null);
                alert.setContentText("New reservation added successfully! Click 'Generate Schedule' to see how the AI handles it.");
                alert.showAndWait();
            }
        });
    }
}