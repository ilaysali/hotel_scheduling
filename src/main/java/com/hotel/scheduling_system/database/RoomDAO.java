package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomDAO extends BaseDAO { // Inherits database connection logic and credentials from BaseDAO

    // Initialize the logger for this class
    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);

    /**
     * Retrieves the complete list of rooms from the hotel database.
     * This data is used to populate the Gantt chart and initialize the scheduling algorithm.
     * @return A list of Room objects containing ID, number, type, and view.
     */
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();

        // SQL query updated to fetch the new view column
        // Make sure 'room_view' matches your actual database column name
        String query = "SELECT room_id, room_number, room_type, room_view FROM Rooms";

        // Using getConnection() from the parent BaseDAO class
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Mapping each database row to a Java Room object with 4 arguments
                rooms.add(new Room(
                        rs.getInt("room_id"),
                        rs.getInt("room_number"),
                        RoomType.valueOf(rs.getString("room_type").toUpperCase()),
                        rs.getString("room_view") // The 4th argument to resolve the error
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve all rooms from the database", e);
            throw new RuntimeException("Database error while fetching rooms", e);
        }
        return rooms;
    }
}