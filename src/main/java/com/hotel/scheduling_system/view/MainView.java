package com.hotel.scheduling_system.view;

import com.hotel.scheduling_system.controller.AppController;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MainView extends VBox {

    private final AppController appController;
    private final GanttChart ganttChart;
    private final UnassignedPanel unassignedPanel;

    // משתנה שישמור את השיבוץ הנוכחי כדי שנוכל לשמור אותו ב-DB
    private Map<Room, List<Reservation>> currentAssignments;

    public MainView(AppController appController) {
        this.appController = appController;
        this.ganttChart = new GanttChart();
        this.unassignedPanel = new UnassignedPanel();

        setPadding(new Insets(20));
        setSpacing(15);

        Label titleLabel = new Label("Hotel Scheduling System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button generateBtn = new Button("Generate Schedule");

        // יצירת כפתור השמירה החדש
        Button saveBtn = new Button("Save Schedule to Database");
        saveBtn.setDisable(true); // מכובה עד שיוצרים שיבוץ
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;"); // צבע ירוק בולט

        HBox buttonsBox = new HBox(15, generateBtn, saveBtn);
        HBox dashboard = new HBox(20, ganttChart, unassignedPanel);

        // מה קורה שלוחצים על יצירת שיבוץ?
        generateBtn.setOnAction(event -> {
            try {
                Map<String, Object> results = appController.onGenerateScheduleClicked();

                currentAssignments = (Map<Room, List<Reservation>>) results.get("ASSIGNMENTS");
                List<Reservation> unassigned = (List<Reservation>) results.get("UNASSIGNED");

                ganttChart.updateData(currentAssignments);
                unassignedPanel.updateData(unassigned);

                // מדליקים את כפתור השמירה!
                if (currentAssignments != null && !currentAssignments.isEmpty()) {
                    saveBtn.setDisable(false);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // מה קורה שלוחצים על שמירה למסד הנתונים?
        saveBtn.setOnAction(event -> {
            if (currentAssignments != null) {
                appController.onSaveScheduleClicked(currentAssignments);

                // חלונית קופצת (Alert) המאשרת למשתמש שהפעולה הצליחה
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Schedule saved successfully to the database!");
                alert.showAndWait();
            }
        });

        getChildren().addAll(titleLabel, buttonsBox, dashboard);
    }
}