package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.HousekeepingTask;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HousekeepingDAO extends BaseDAO { // Inherits database connection logic from BaseDAO

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
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            int staffIndex = 0;
            for (int roomId : roomIds) {
                pstmt.setInt(1, roomId);
                // Round-robin logic: assign room to the next staff member in the list
                pstmt.setInt(2, staffIds.get(staffIndex % staffIds.size()));
                pstmt.setDate(3, java.sql.Date.valueOf(date));
                pstmt.addBatch();
                staffIndex++;
            }
            pstmt.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
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
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
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
            e.printStackTrace();
        }
        return tasks;
    }

    /**
     * Helper method to remove existing tasks for a specific date from the schedule.
     */
    private void clearTasksForDate(LocalDate date) {
        String query = "DELETE FROM Housekeeping_Schedule WHERE cleaning_date = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}