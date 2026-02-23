package com.hotel.scheduling_system.model;

public record Guest(int id, String firstName, String lastName) {
    // דריסת הפונקציה כדי שברשימה הנפתחת יוצג השם המלא של האורח ולא קוד ג'יבריש
    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}