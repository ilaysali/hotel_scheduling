package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Staff;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StaffDAO extends BaseDAO { // Inherits database connection logic and credentials from BaseDAO

    /**
     * Retrieves all staff members assigned to the 'Housekeeping' role.
     * This list is used to distribute cleaning tasks across available personnel.
     * @return A list of Staff objects with the Housekeeping role.
     */
    public List<Staff> getHousekeepingStaff() {
        List<Staff> staffList = new ArrayList<>();

        // Query to filter specifically for staff members in the Housekeeping department
        String query = "SELECT staff_id, first_name, last_name, role FROM Staff WHERE role = 'Housekeeping'";

        // Using getConnection() from the parent BaseDAO class
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Mapping the database result set to Staff domain objects
                staffList.add(new Staff(
                        rs.getInt("staff_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            // Log any SQL errors to the console for debugging
            e.printStackTrace();
        }
        return staffList;
    }
}