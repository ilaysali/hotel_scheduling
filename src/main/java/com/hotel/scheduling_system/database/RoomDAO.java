package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.RoomType;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomDAO extends BaseDAO { // Inherits database connection logic and credentials from BaseDAO

    /**
     * Retrieves the complete list of rooms from the hotel database.
     * This data is used to populate the Gantt chart and initialize the scheduling algorithm.
     * @return A list of Room objects containing ID, number, and type.
     */
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();

        // SQL query to fetch basic room information
        String query = "SELECT room_id, room_number, room_type FROM Rooms";

        // Using getConnection() from the parent BaseDAO class
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Mapping each database row to a Java Room object
                rooms.add(new Room(
                        rs.getInt("room_id"),
                        rs.getInt("room_number"),
                        // Converts the string from the database to a RoomType Enum (case-insensitive)
                        RoomType.valueOf(rs.getString("room_type").toUpperCase())
                ));
            }
        } catch (SQLException e) {
            // Logs database errors for debugging
            e.printStackTrace();
        }
        return rooms;
    }
}