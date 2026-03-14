package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.HousekeepingTask;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.ScenarioLoaderService; // הוספנו את ה-Import
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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MainView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final AppController appController;
    private final ScenarioLoaderService scenarioLoaderService;
    private final GanttChart ganttChart;
    private final UnassignedPanel unassignedPanel;
    private final Button saveBtn;
    private final Label fitnessLabel;

    private Map<Room, List<Reservation>> currentAssignments;
    private List<Reservation> currentUnassigned;
    private final Set<Reservation> approvedDowngrades = new HashSet<>();

    public MainView(AppController appController, ScenarioLoaderService scenarioLoaderService) {
        this.appController = appController;
        this.scenarioLoaderService = scenarioLoaderService;
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

        fitnessLabel = new Label("AI Fitness Score: Waiting...");
        fitnessLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #673AB7; -fx-background-color: #EDE7F6; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");

        headerBox.getChildren().addAll(titleLabel, spacer, fitnessLabel);

        MenuButton loadDataBtn = new MenuButton("1. Load Data Scenario");
        loadDataBtn.setStyle("-fx-base: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");

        MenuItem heavyMockItem = new MenuItem("Load Heavy Scenario (JSON)");
        MenuItem lightMockItem = new MenuItem("Load Light Scenario (JSON)");
        MenuItem customJsonItem = new MenuItem("Upload Custom JSON...");

        loadDataBtn.getItems().addAll(heavyMockItem, lightMockItem, new SeparatorMenuItem(), customJsonItem);

        Button generateBtn = new Button("2. Generate Schedule");

        saveBtn = new Button("3. Save Schedule to Database");
        saveBtn.setDisable(true);
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        Button addResBtn = new Button("➕ New Reservation");
        addResBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        Button housekeepingBtn = new Button("Housekeeping");
        housekeepingBtn.setStyle("-fx-base: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonsBox = new HBox(15, loadDataBtn, generateBtn, saveBtn, addResBtn, housekeepingBtn);
        HBox dashboard = new HBox(20, ganttChart, unassignedPanel);

        heavyMockItem.setOnAction(e -> {
            try {
                // Read heavy scenario from classpath using InputStream
                java.io.InputStream is = getClass().getResourceAsStream("/heavy_scenario.json");
                if (is == null) {
                    throw new java.io.FileNotFoundException("Could not find heavy_scenario.json in resources");
                }
                scenarioLoaderService.loadScenarioFromStream(is);
                resetDashboardState("Heavy JSON scenario loaded successfully!");
            } catch (Exception ex) {
                logger.error("Failed to load heavy scenario", ex);
                new Alert(Alert.AlertType.ERROR, "Failed to load heavy scenario: " + ex.getMessage()).showAndWait();
            }
        });

        lightMockItem.setOnAction(e -> {
            try {
                // Read from classpath using InputStream
                java.io.InputStream is = getClass().getResourceAsStream("/light_scenario.json");
                if (is == null) {
                    throw new java.io.FileNotFoundException("Could not find light_scenario.json in resources");
                }
                scenarioLoaderService.loadScenarioFromStream(is);
                resetDashboardState("Light JSON scenario loaded successfully!");
            } catch (Exception ex) {
                logger.error("Failed to load light scenario", ex);
                // Print the exact message to the alert for easier debugging
                new Alert(Alert.AlertType.ERROR, "Failed to load light scenario: " + ex.getMessage()).showAndWait();
            }
        });

        customJsonItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Custom JSON Scenario");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

            File selectedFile = fileChooser.showOpenDialog(this.getScene().getWindow());

            if (selectedFile != null) {
                try {
                    // כאן אנחנו משתמשים ב-Service החדש עבור הקובץ שהמשתמש בחר
                    scenarioLoaderService.loadScenarioFromFile(selectedFile);
                    resetDashboardState("Custom JSON scenario loaded successfully from:\n" + selectedFile.getName());
                } catch (Exception ex) {
                    logger.error("Failed to load custom scenario", ex);
                    new Alert(Alert.AlertType.ERROR, "Failed to load the selected file. Ensure the JSON format is correct.").showAndWait();
                }
            }
        });

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

        generateBtn.setOnAction(event -> {
            try {
                Map<String, Object> results = appController.onGenerateScheduleClicked();

                @SuppressWarnings("unchecked")
                Map<Room, List<Reservation>> assignments = (Map<Room, List<Reservation>>) results.get("ASSIGNMENTS");
                currentAssignments = assignments;

                @SuppressWarnings("unchecked")
                List<Reservation> unassigned = (List<Reservation>) results.get("UNASSIGNED");
                currentUnassigned = unassigned;

                Double fitnessScore = (Double) results.get("FITNESS_SCORE");

                approvedDowngrades.clear();
                refreshDashboard();
                if (fitnessScore != null) fitnessLabel.setText(String.format("AI Fitness Score: %,.0f", fitnessScore));

            } catch (IllegalArgumentException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                errorAlert.showAndWait();
            } catch (Exception e) {
                logger.error("Error occurred while generating the AI schedule", e);
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, "An unexpected error occurred while generating the schedule.");
                errorAlert.showAndWait();
            }
        });

        saveBtn.setOnAction(event -> {
            if (currentAssignments != null) {
                appController.onSaveScheduleClicked(currentAssignments);
                new Alert(Alert.AlertType.INFORMATION, "Schedule saved to Database!").showAndWait();
            }
        });

        getChildren().addAll(headerBox, buttonsBox, dashboard);
    }

    private void resetDashboardState(String successMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Database Update");
        alert.setHeaderText(null);
        alert.setContentText(successMessage + "\nRun the AI to see the schedule.");
        alert.showAndWait();

        fitnessLabel.setText("AI Fitness Score: Waiting...");
        approvedDowngrades.clear();
        saveBtn.setDisable(true);

        // Initialize to empty collections if they are null to prevent NullPointerException
        if (currentAssignments == null) {
            currentAssignments = new java.util.HashMap<>();
        }
        if (currentUnassigned == null) {
            currentUnassigned = new java.util.ArrayList<>();
        }

        // Clear existing data
        currentAssignments.clear();
        currentUnassigned.clear();

        // Update the UI components with empty data
        ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);
    }
    private void refreshDashboard() {
        if (currentAssignments == null) return;
        boolean hasPendingDowngrades = ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);
        saveBtn.setDisable(currentAssignments.isEmpty() || hasPendingDowngrades);
    }

    private void openHousekeepingDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Daily Housekeeping Report");
        dialog.setHeaderText("View and assign cleaning tasks based on AI Schedule");

        VBox vbox = new VBox(10);
        HBox topControls = new HBox(10);
        topControls.setAlignment(Pos.CENTER_LEFT);

        DatePicker datePicker = new DatePicker(LocalDate.of(2026, 4, 10));
        Button loadBtn = new Button("Generate Tasks");
        loadBtn.setStyle("-fx-base: #FFC107; -fx-font-weight: bold;");

        topControls.getChildren().addAll(new Label("Select Date:"), datePicker, loadBtn);

        TableView<HousekeepingTask> table = new TableView<>();

        TableColumn<HousekeepingTask, Integer> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().roomNumber()).asObject());

        TableColumn<HousekeepingTask, String> staffCol = new TableColumn<>("Assigned Staff");
        staffCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().staffName()));

        TableColumn<HousekeepingTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));

        table.getColumns().add(roomCol);
        table.getColumns().add(staffCol);
        table.getColumns().add(statusCol);

        table.setPrefWidth(350);
        table.setPrefHeight(300);

        loadBtn.setOnAction(e -> {
            LocalDate date = datePicker.getValue();
            if (date != null) {
                List<Integer> activeRooms = new ArrayList<>();
                if (currentAssignments != null) {
                    for (Map.Entry<Room, List<Reservation>> entry : currentAssignments.entrySet()) {
                        boolean needsCleaning = entry.getValue().stream().anyMatch(res ->
                                !date.isBefore(res.startDate()) && !date.isAfter(res.endDate())
                        );
                        if (needsCleaning) {
                            activeRooms.add(entry.getKey().id());
                        }
                    }
                }
                if (activeRooms.isEmpty()) {
                    activeRooms.addAll(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
                }
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

        ComboBox<String> viewPreferenceBox = new ComboBox<>();
        viewPreferenceBox.getItems().addAll("Any", "Sea", "Pool", "City", "Garden");
        viewPreferenceBox.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Guest Name:"), 0, 0); grid.add(guestComboBox, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1); grid.add(startDatePicker, 1, 1);
        grid.add(new Label("End Date:"), 0, 2); grid.add(endDatePicker, 1, 2);
        grid.add(new Label("Requested Room:"), 0, 3); grid.add(roomTypeBox, 1, 3);
        grid.add(new Label("Preferred View:"), 0, 4); grid.add(viewPreferenceBox, 1, 4);

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
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int guestId = appController.getOrCreateGuest(guestComboBox.getValue());
                String selectedView = viewPreferenceBox.getValue();
                String finalPreferredView = "Any".equals(selectedView) ? null : selectedView;

                appController.createNewReservation(
                        guestId,
                        roomTypeBox.getValue(),
                        startDatePicker.getValue(),
                        endDatePicker.getValue(),
                        finalPreferredView
                );

                new Alert(Alert.AlertType.INFORMATION, "New reservation added! Click 'Generate Schedule' to see how the AI handles it.").showAndWait();
            }
        });
    }
}