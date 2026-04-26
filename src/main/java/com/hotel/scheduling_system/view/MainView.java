package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.controller.PostProcessor.ProcessingResult;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.ScenarioLoaderService;
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
import java.io.File;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MainView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final AppController appController;
    private final GanttChart ganttChart;
    private final UnassignedPanel unassignedPanel;
    private final AllReservationsPanel allReservationsPanel;
    private final Button saveBtn;
    private final Label fitnessLabel;

    private Map<Room, List<Reservation>> currentAssignments;
    private List<Reservation> currentUnassigned;
    private final Set<Reservation> approvedDowngrades = new HashSet<>();

    /**
     * Constructs the main application view.
     * Initializes components and orchestrates layout setup and event binding.
     */
    public MainView(AppController appController, ScenarioLoaderService scenarioLoaderService) {
        this.appController = appController;
        this.ganttChart = new GanttChart();
        this.unassignedPanel = new UnassignedPanel();
        this.allReservationsPanel = new AllReservationsPanel();

        this.fitnessLabel = createFitnessLabel();
        this.saveBtn = createSaveButton();

        // Ensure the panel starts empty regardless of what is in the database
        allReservationsPanel.updateData(new java.util.ArrayList<>());

        setPadding(new Insets(20));
        setSpacing(15);

        HBox headerBox = buildHeader();

        Button generateBtn = new Button("2. Generate Schedule");
        Button addResBtn = createAddReservationButton();
        MenuButton loadDataBtn = buildLoadDataMenu(scenarioLoaderService);

        HBox buttonsBox = new HBox(15, loadDataBtn, generateBtn, saveBtn, addResBtn);

        GridPane dashboard = buildDashboard();

        setupActionHandlers(generateBtn, addResBtn);

        getChildren().addAll(headerBox, buttonsBox, dashboard);
    }

    /**
     * Creates and styles the label displaying the AI fitness score.
     */
    private Label createFitnessLabel() {
        Label label = new Label("AI Fitness Score: Waiting...");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #673AB7; -fx-background-color: #EDE7F6; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        return label;
    }

    /**
     * Creates and styles the button used to save the schedule to the database.
     */
    private Button createSaveButton() {
        Button btn = new Button("3. Save Schedule to Database");
        btn.setDisable(true);
        btn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        return btn;
    }

    /**
     * Creates and styles the button used to add a new reservation.
     */
    private Button createAddReservationButton() {
        Button btn = new Button("➕ New Reservation");
        btn.setStyle("-fx-base: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        return btn;
    }

    /**
     * Builds the top header layout containing the title and fitness score.
     */
    private HBox buildHeader() {
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Hotel Scheduling System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(titleLabel, spacer, fitnessLabel);
        return headerBox;
    }

    /**
     * Builds the central dashboard grid layout comprising the Gantt chart and side panels.
     */
    private GridPane buildDashboard() {
        GridPane dashboard = new GridPane();
        dashboard.setHgap(15);
        VBox.setVgrow(dashboard, Priority.ALWAYS);

        // Column 0: Gantt Chart
        javafx.scene.layout.ColumnConstraints colGantt = new javafx.scene.layout.ColumnConstraints();
        colGantt.setHgrow(Priority.ALWAYS);
        colGantt.setMinWidth(400);

        // Column 1: Unassigned Panel
        javafx.scene.layout.ColumnConstraints colUnassigned = new javafx.scene.layout.ColumnConstraints();
        colUnassigned.setMinWidth(250);
        colUnassigned.setMaxWidth(250);
        colUnassigned.setPrefWidth(250);

        // Column 2: All Reservations Panel
        javafx.scene.layout.ColumnConstraints colAllRes = new javafx.scene.layout.ColumnConstraints();
        colAllRes.setMinWidth(300);
        colAllRes.setMaxWidth(300);
        colAllRes.setPrefWidth(300);

        dashboard.getColumnConstraints().addAll(colGantt, colUnassigned, colAllRes);

        dashboard.add(ganttChart, 0, 0);
        dashboard.add(unassignedPanel, 1, 0);
        dashboard.add(allReservationsPanel, 2, 0);

        GridPane.setVgrow(ganttChart, Priority.ALWAYS);
        GridPane.setVgrow(unassignedPanel, Priority.ALWAYS);
        GridPane.setVgrow(allReservationsPanel, Priority.ALWAYS);

        return dashboard;
    }

    /**
     * Builds the drop-down menu button for loading various data scenarios.
     */
    private MenuButton buildLoadDataMenu(ScenarioLoaderService scenarioLoaderService) {
        MenuButton loadDataBtn = new MenuButton("1. Load Data Scenario");
        loadDataBtn.setStyle("-fx-base: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");

        MenuItem heavyMockItem = new MenuItem("Load Heavy Scenario (JSON)");
        MenuItem lightMockItem = new MenuItem("Load Light Scenario (JSON)");
        MenuItem customJsonItem = new MenuItem("Upload Custom JSON...");

        heavyMockItem.setOnAction(e -> loadScenarioFromResource("/heavy_scenario.json", "Heavy JSON scenario loaded successfully!", scenarioLoaderService));
        lightMockItem.setOnAction(e -> loadScenarioFromResource("/light_scenario.json", "Light JSON scenario loaded successfully!", scenarioLoaderService));
        customJsonItem.setOnAction(e -> loadCustomScenario(scenarioLoaderService));

        loadDataBtn.getItems().addAll(heavyMockItem, lightMockItem, new SeparatorMenuItem(), customJsonItem);
        return loadDataBtn;
    }

    /**
     * Loads a scenario from a resource path inside the jar and updates the UI.
     */
    private void loadScenarioFromResource(String resourcePath, String successMessage, ScenarioLoaderService scenarioLoaderService) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new java.io.FileNotFoundException("Could not find " + resourcePath + " in resources");
            }
            scenarioLoaderService.loadScenarioFromStream(is);
            resetDashboardState(successMessage);
            allReservationsPanel.updateData(appController.getAllReservations());
        } catch (Exception ex) {
            logger.error("Failed to load scenario {}", resourcePath, ex);
            new Alert(Alert.AlertType.ERROR, "Failed to load scenario: " + ex.getMessage()).showAndWait();
        }
    }

    /**
     * Prompts the user to select a custom JSON file and loads the scenario.
     */
    private void loadCustomScenario(ScenarioLoaderService scenarioLoaderService) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Custom JSON Scenario");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(this.getScene().getWindow());

        if (selectedFile != null) {
            try {
                scenarioLoaderService.loadScenarioFromFile(selectedFile);
                resetDashboardState("Custom JSON scenario loaded successfully from:\n" + selectedFile.getName());
                allReservationsPanel.updateData(appController.getAllReservations());
            } catch (Exception ex) {
                logger.error("Failed to load custom scenario", ex);
                new Alert(Alert.AlertType.ERROR, "Failed to load the selected file. Ensure the JSON format is correct.").showAndWait();
            }
        }
    }

    /**
     * Sets up event handlers for user actions such as schedule generation and Gantt chart interactions.
     */
    private void setupActionHandlers(Button generateBtn, Button addResBtn) {
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

        generateBtn.setOnAction(event -> handleGenerateSchedule());

        saveBtn.setOnAction(event -> {
            if (currentAssignments != null) {
                appController.onSaveScheduleClicked(currentAssignments);
                new Alert(Alert.AlertType.INFORMATION, "Schedule saved to Database!").showAndWait();
            }
        });
    }

    /**
     * Handles the process of triggering the AI to generate a schedule and updating the UI with results.
     */
    private void handleGenerateSchedule() {
        try {
            ProcessingResult results = appController.onGenerateScheduleClicked();

            currentAssignments = results.assignments();
            currentUnassigned = results.unassigned();
            Double fitnessScore = results.fitnessScore();

            approvedDowngrades.clear();
            refreshDashboard();
            fitnessLabel.setText(String.format("AI Fitness Score: %,.0f", fitnessScore));

        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (Exception e) {
            logger.error("Error occurred while generating the AI schedule", e);
            new Alert(Alert.AlertType.ERROR, "An unexpected error occurred while generating the schedule.").showAndWait();
        }
    }

    /**
     * Resets the dashboard panels and internal state when new data is loaded.
     */
    private void resetDashboardState(String successMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Database Update");
        alert.setHeaderText(null);
        alert.setContentText(successMessage + "\nRun the AI to see the schedule.");
        alert.showAndWait();

        fitnessLabel.setText("AI Fitness Score: Waiting...");
        approvedDowngrades.clear();
        saveBtn.setDisable(true);

        if (currentAssignments == null) {
            currentAssignments = new java.util.HashMap<>();
        }
        if (currentUnassigned == null) {
            currentUnassigned = new java.util.ArrayList<>();
        }

        currentAssignments.clear();
        currentUnassigned.clear();

        ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);
    }

    /**
     * Refreshes the Gantt chart and side panels based on current internal assignment data.
     */
    private void refreshDashboard() {
        if (currentAssignments == null) return;
        boolean hasPendingDowngrades = ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);

        java.util.List<Reservation> allRes = new java.util.ArrayList<>();
        for (List<Reservation> roomSchedule : currentAssignments.values()) {
            allRes.addAll(roomSchedule);
        }
        if (currentUnassigned != null) {
            allRes.addAll(currentUnassigned);
        }
        allRes.sort(java.util.Comparator.comparingInt(Reservation::id));
        allReservationsPanel.updateData(allRes);

        saveBtn.setDisable(currentAssignments.isEmpty() || hasPendingDowngrades);
    }

    /**
     * Opens a dialog allowing the user to create a new reservation.
     */
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

                // Update All Reservations Panel immediately after adding a new reservation
                allReservationsPanel.updateData(appController.getAllReservations());

                new Alert(Alert.AlertType.INFORMATION, "New reservation added! Click 'Generate Schedule' to see how the AI handles it.").showAndWait();
            }
        });
    }
}