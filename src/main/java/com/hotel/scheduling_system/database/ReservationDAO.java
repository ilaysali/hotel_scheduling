package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.RoomType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    // Configuration properties should ideally be in db_config.properties
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "password";

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT id, start_date, end_date, room_type FROM reservations";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getTimestamp("start_date").toLocalDateTime().toLocalDate(),
                        rs.getTimestamp("end_date").toLocalDateTime().toLocalDate(),
                        RoomType.valueOf(rs.getString("room_type"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }
}