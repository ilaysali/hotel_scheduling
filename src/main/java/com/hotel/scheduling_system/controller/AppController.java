package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.database.MockDataGenerator;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.SchedulingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AppController {

    private final SchedulingService schedulingService;
    private final MockDataGenerator mockDataGenerator;

    // הזרקנו גם את מחולל הנתונים
    public AppController(SchedulingService schedulingService, MockDataGenerator mockDataGenerator) {
        this.schedulingService = schedulingService;
        this.mockDataGenerator = mockDataGenerator;
    }

    public Map<String, Object> onGenerateScheduleClicked() {
        return schedulingService.generateSchedule();
    }

    public void onSaveScheduleClicked(Map<Room, List<Reservation>> assignments) {
        schedulingService.saveScheduleToDatabase(assignments);
    }

    // הפונקציה החדשה שתופעל מהממשק
    public void onLoadMockDataClicked() {
        mockDataGenerator.resetAndGenerate();
    }
}