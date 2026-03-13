package com.hotel.scheduling_system.dto;
import java.util.List;
public record ScenarioDTO(List<RoomDTO> rooms, List<GuestDTO> guests, List<ReservationDTO> reservations) {}