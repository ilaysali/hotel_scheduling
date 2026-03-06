package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Guest;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GuestDAO extends BaseDAO { // Inherits database credentials and connection logic from BaseDAO

    /**
     * Retrieves all guests from the database.
     * @return A list of Guest objects.
     */
    public List<Guest> getAllGuests() {
        List<Guest> guests = new ArrayList<>();
        String query = "SELECT guest_id, first_name, last_name FROM Guests";

        // Using getConnection() from the parent class (BaseDAO)
        try (Connection conn = getConnection();
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

    /**
     * Logic for handling free-text guest entry.
     * Finds an existing guest by name or creates a new one if not found.
     * @param fullName The full name entered by the user.
     * @return The ID of the guest.
     */
    public int getOrCreateGuest(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        // 1. Check if the guest already exists
        String findQuery = "SELECT guest_id FROM Guests WHERE first_name = ? AND last_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(findQuery)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("guest_id"); // Found - returning existing ID
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // 2. If not found - calculate the next available ID
        int nextId = 1;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(guest_id), 0) + 1 AS next_id FROM Guests")) {
            if (rs.next()) nextId = rs.getInt("next_id");
        } catch (SQLException e) { e.printStackTrace(); }

        // 3. Save the new guest to the database
        String insertQuery = "INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, "new_guest@example.com"); // Default email for new entries
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        return nextId;
    }
}