package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.SchedulingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AppController {

    private final SchedulingService schedulingService;

    public AppController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    public Map<String, Object> onGenerateScheduleClicked() {
        return schedulingService.generateSchedule();
    }

    // הניתוב החדש עבור כפתור השמירה
    public void onSaveScheduleClicked(Map<Room, List<Reservation>> assignments) {
        schedulingService.saveScheduleToDatabase(assignments);
    }
}