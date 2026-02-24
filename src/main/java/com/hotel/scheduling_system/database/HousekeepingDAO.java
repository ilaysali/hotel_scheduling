package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.HousekeepingTask;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HousekeepingDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    // פעולה שמחלקת משימות ניקיון לעובדים באופן שווה (Round-robin)
    public void assignTasksForDate(LocalDate date, List<Integer> roomIds, List<Integer> staffIds) {
        if (staffIds.isEmpty() || roomIds.isEmpty()) return;

        // מוחקים משימות ישנות לאותו יום כדי לא ליצור כפילויות אם לוחצים פעמיים
        clearTasksForDate(date);

        // תיקון: שימוש ב-cleaning_date
        String insertQuery = "INSERT INTO Housekeeping_Schedule (room_id, staff_id, cleaning_date, status) VALUES (?, ?, ?, 'PENDING')";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            int staffIndex = 0;
            for (int roomId : roomIds) {
                pstmt.setInt(1, roomId);
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

    public List<HousekeepingTask> getTasksForDate(LocalDate date) {
        List<HousekeepingTask> tasks = new ArrayList<>();

        // תיקון: שימוש ב-schedule_id ו-cleaning_date
        String query = """
                SELECT h.schedule_id, r.room_number, CONCAT(s.first_name, ' ', s.last_name) AS staff_name, h.cleaning_date, h.status
                FROM Housekeeping_Schedule h
                JOIN Rooms r ON h.room_id = r.room_id
                JOIN Staff s ON h.staff_id = s.staff_id
                WHERE h.cleaning_date = ?
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new HousekeepingTask(
                            rs.getInt("schedule_id"), // הותאם לשם העמודה שלך
                            rs.getInt("room_number"),
                            rs.getString("staff_name"),
                            rs.getDate("cleaning_date").toLocalDate(), // הותאם לשם העמודה שלך
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    private void clearTasksForDate(LocalDate date) {
        // תיקון: שימוש ב-cleaning_date
        String query = "DELETE FROM Housekeeping_Schedule WHERE cleaning_date = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}