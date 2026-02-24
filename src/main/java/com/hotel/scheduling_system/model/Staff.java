package com.hotel.scheduling_system.model;

public record Staff(int id, String firstName, String lastName, String role) {
    @Override
    public String toString() {
        return firstName + " " + lastName + " (" + role + ")";
    }
}