package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.HousekeepingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HousekeepingDAO extends BaseDAO { // Inherits database connection logic from BaseDAO

    // Initialize the logger for this class
    private static final Logger logger = LoggerFactory.getLogger(HousekeepingDAO.class);

    /**
     * Assigns cleaning tasks to staff members using a Round-robin algorithm.
     * Prevents duplicates by clearing existing tasks for the given date first.
     */
    public void assignTasksForDate(LocalDate date, List<Integer> roomIds, List<Integer> staffIds) {
        if (staffIds.isEmpty() || roomIds.isEmpty()) return;

        // Delete old tasks for this date to avoid duplicates if re-assigned
        clearTasksForDate(date);

        // SQL query to insert new housekeeping assignments
        String insertQuery = "INSERT INTO Housekeeping_Schedule (room_id, staff_id, cleaning_date, status) VALUES (?, ?, ?, 'PENDING')";

        // Using getConnection() from BaseDAO to establish the link
        try (Connection conn = getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(insertQuery)) {

            int staffIndex = 0;
            for (int roomId : roomIds) {
                preparedStatement.setInt(1, roomId);
                // Round-robin logic: assign room to the next staff member in the list
                preparedStatement.setInt(2, staffIds.get(staffIndex % staffIds.size()));
                preparedStatement.setDate(3, java.sql.Date.valueOf(date));
                preparedStatement.addBatch();
                staffIndex++;
            }
            preparedStatement.executeBatch();

        } catch (SQLException e) {
            logger.error("Failed to assign housekeeping tasks for date: {}", date, e);
            throw new RuntimeException("Database error while assigning tasks", e);
        }
    }

    /**
     * Retrieves all housekeeping tasks for a specific date, including room and staff details.
     */
    public List<HousekeepingTask> getTasksForDate(LocalDate date) {
        List<HousekeepingTask> tasks = new ArrayList<>();

        // Join query to fetch human-readable room numbers and staff names
        String query = """
                SELECT h.schedule_id, r.room_number, CONCAT(s.first_name, ' ', s.last_name) AS staff_name, h.cleaning_date, h.status
                FROM Housekeeping_Schedule h
                JOIN Rooms r ON h.room_id = r.room_id
                JOIN Staff s ON h.staff_id = s.staff_id
                WHERE h.cleaning_date = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new HousekeepingTask(
                            rs.getInt("schedule_id"),
                            rs.getInt("room_number"),
                            rs.getString("staff_name"),
                            rs.getDate("cleaning_date").toLocalDate(),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve housekeeping tasks for date: {}", date, e);
            throw new RuntimeException("Database error while fetching tasks", e);
        }
        return tasks;
    }

    /**
     * Helper method to remove existing tasks for a specific date from the schedule.
     */
    private void clearTasksForDate(LocalDate date) {
        String query = "DELETE FROM Housekeeping_Schedule WHERE cleaning_date = ?";
        try (Connection conn = getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setDate(1, java.sql.Date.valueOf(date));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to clear existing tasks for date: {}", date, e);
            throw new RuntimeException("Database error while clearing old tasks", e);
        }
    }
}