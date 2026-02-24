package com.hotel.scheduling_system.model;

import java.time.LocalDate;

public record HousekeepingTask(int taskId, int roomNumber, String staffName, LocalDate taskDate, String status) {
}