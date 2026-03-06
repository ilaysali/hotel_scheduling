package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.HousekeepingTask;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.ArrayList;
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

        Button addResBtn = new Button("➕ New Reservation");
        addResBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        Button housekeepingBtn = new Button("Housekeeping");
        housekeepingBtn.setStyle("-fx-base: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonsBox = new HBox(15, mockBtn, generateBtn, saveBtn, addResBtn, housekeepingBtn);
        HBox dashboard = new HBox(20, ganttChart, unassignedPanel);

        housekeepingBtn.setOnAction(e -> openHousekeepingDialog());
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
            alert.setContentText("Mock data loaded! Run the AI to see the schedule.");
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
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                errorAlert.showAndWait();
            } catch (Exception e) { e.printStackTrace(); }
        });

        saveBtn.setOnAction(event -> {
            if (currentAssignments != null) {
                appController.onSaveScheduleClicked(currentAssignments);
                new Alert(Alert.AlertType.INFORMATION, "Schedule saved to Database!").showAndWait();
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

    // --- חלון דו"ח הניקיונות המטורף שלנו ---
    private void openHousekeepingDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Daily Housekeeping Report");
        dialog.setHeaderText("View and assign cleaning tasks based on AI Schedule");

        VBox vbox = new VBox(10);
        HBox topControls = new HBox(10);
        topControls.setAlignment(Pos.CENTER_LEFT);

        DatePicker datePicker = new DatePicker(LocalDate.of(2026, 4, 10)); // תאריך ברירת מחדל מהעומס שלנו
        Button loadBtn = new Button("Generate Tasks");
        loadBtn.setStyle("-fx-base: #FFC107; -fx-font-weight: bold;");

        topControls.getChildren().addAll(new Label("Select Date:"), datePicker, loadBtn);

        // טבלת משימות מקצועית (TableView)
        TableView<HousekeepingTask> table = new TableView<>();

        TableColumn<HousekeepingTask, Integer> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().roomNumber()).asObject());

        TableColumn<HousekeepingTask, String> staffCol = new TableColumn<>("Assigned Staff");
        staffCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().staffName()));

        TableColumn<HousekeepingTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));

        table.getColumns().addAll(roomCol, staffCol, statusCol);
        table.setPrefWidth(350);
        table.setPrefHeight(300);

        loadBtn.setOnAction(e -> {
            LocalDate date = datePicker.getValue();
            if (date != null) {
                List<Integer> activeRooms = new ArrayList<>();

                // ה"קסם": סורקים את התוצאות שה-AI קבע למלון!
                if (currentAssignments != null) {
                    for (Map.Entry<Room, List<Reservation>> entry : currentAssignments.entrySet()) {
                        // אם יש בחדר הזה הזמנה שנופלת על התאריך הנבחר - הוא דורש ניקיון!
                        boolean needsCleaning = entry.getValue().stream().anyMatch(res ->
                                !date.isBefore(res.startDate()) && !date.isAfter(res.endDate())
                        );
                        if (needsCleaning) {
                            activeRooms.add(entry.getKey().getId());
                        }
                    }
                }

                // מנגנון ביטחון: אם עוד לא יצרנו לוח חכם, ניתן משימות לכל החדרים כברירת מחדל
                if (activeRooms.isEmpty()) {
                    activeRooms.addAll(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
                }

                // שולחים ל-Controller לייצר ולשמור את המשימות במסד הנתונים
                List<HousekeepingTask> tasks = appController.generateAndGetHousekeepingReport(date, activeRooms);
                table.getItems().setAll(tasks);
            }
        });

        vbox.getChildren().addAll(topControls, table);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void openNewReservationDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Reservation");
        dialog.setHeaderText("Please fill out the reservation details:");

        ComboBox<String> guestComboBox = new ComboBox<>();
        for (Guest g : appController.getAllGuests()) {
            guestComboBox.getItems().add(g.firstName() + " " + g.lastName());
        }
        guestComboBox.setEditable(true);
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

        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String guestName = guestComboBox.getValue();
            String roomType = roomTypeBox.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (guestName == null || guestName.trim().isEmpty() || roomType == null) {
                new Alert(Alert.AlertType.ERROR, "You must provide a guest name and select a room type.").showAndWait();
                event.consume(); return;
            }
            if (start == null || end == null || !end.isAfter(start)) {
                new Alert(Alert.AlertType.ERROR, "The End Date must be strictly after the Start Date.").showAndWait();
                event.consume(); return;
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int guestId = appController.getOrCreateGuest(guestComboBox.getValue());
                appController.createNewReservation(guestId, roomTypeBox.getValue(), startDatePicker.getValue(), endDatePicker.getValue());
                new Alert(Alert.AlertType.INFORMATION, "New reservation added! Click 'Generate Schedule' to see how the AI handles it.").showAndWait();
            }
        });
    }
}