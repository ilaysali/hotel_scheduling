package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;

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

    // הרשימה שתשמור איזה מהשנמוכים המנהל כבר אישר
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

        HBox buttonsBox = new HBox(15, mockBtn, generateBtn, saveBtn);
        HBox dashboard = new HBox(20, ganttChart, unassignedPanel);

        // --- לוגיקת האישורים והדחיות ---
        ganttChart.setOnRejectCallback(rejectedRes -> {
            if (currentAssignments != null && currentUnassigned != null) {
                for (List<Reservation> roomSchedule : currentAssignments.values()) {
                    if (roomSchedule.remove(rejectedRes)) break;
                }
                currentUnassigned.add(rejectedRes);
                approvedDowngrades.remove(rejectedRes); // ליתר ביטחון ננקה מהאישורים
                refreshDashboard();
            }
        });

        ganttChart.setOnApproveCallback(approvedRes -> {
            approvedDowngrades.add(approvedRes); // המנהל לקח אחריות
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

                approvedDowngrades.clear(); // איפוס אישורים לשיבוץ חדש

                refreshDashboard();

                if (fitnessScore != null) {
                    fitnessLabel.setText(String.format("AI Fitness Score: %,.0f", fitnessScore));
                }

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

    // פונקציית עזר לציור מחדש ובדיקת חסימת כפתור השמירה
    private void refreshDashboard() {
        if (currentAssignments == null) return;

        // מעדכנים את הלוח ובודקים אם נשארו שנמוכים מסוכנים
        boolean hasPendingDowngrades = ganttChart.updateData(currentAssignments, approvedDowngrades);
        unassignedPanel.updateData(currentUnassigned);

        // פותחים את כפתור השמירה רק אם אין בעיות תלויות
        if (!currentAssignments.isEmpty() && !hasPendingDowngrades) {
            saveBtn.setDisable(false);
        } else {
            saveBtn.setDisable(true);
        }
    }
}