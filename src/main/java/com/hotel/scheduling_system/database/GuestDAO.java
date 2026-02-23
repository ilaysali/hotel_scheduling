package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Guest;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GuestDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    public List<Guest> getAllGuests() {
        List<Guest> guests = new ArrayList<>();
        String query = "SELECT guest_id, first_name, last_name FROM Guests";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                guests.add(new Guest(
                        rs.getInt("guest_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return guests;
    }

    // --- הפעולה החדשה שמטפלת בהקלדת טקסט חופשי ---
    public int getOrCreateGuest(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        // 1. בודקים אם האורח כבר קיים
        String findQuery = "SELECT guest_id FROM Guests WHERE first_name = ? AND last_name = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(findQuery)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("guest_id"); // נמצא - מחזירים את המזהה הקיים
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // 2. אם לא נמצא - יוצרים מזהה חדש
        int nextId = 1;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(guest_id), 0) + 1 AS next_id FROM Guests")) {
            if (rs.next()) nextId = rs.getInt("next_id");
        } catch (SQLException e) { e.printStackTrace(); }

        // 3. שומרים את האורח החדש במסד הנתונים
        String insertQuery = "INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, "new_guest@example.com"); // אימייל ברירת מחדל
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        return nextId;
    }
}