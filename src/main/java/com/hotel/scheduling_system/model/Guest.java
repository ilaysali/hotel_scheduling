package com.hotel.scheduling_system.model;

public record Guest(int id, String firstName, String lastName) {
    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}