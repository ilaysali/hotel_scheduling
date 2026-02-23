package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.RoomType;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin"; // Using your correct password!

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();

        // Updated query to use the new 'room_id' column
        String query = "SELECT room_id, room_number, room_type FROM Rooms";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                rooms.add(new Room(
                        rs.getInt("room_id"), // Extract using the new column name
                        rs.getInt("room_number"),
                        RoomType.valueOf(rs.getString("room_type"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
}