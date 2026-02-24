package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Staff;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StaffDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    public List<Staff> getHousekeepingStaff() {
        List<Staff> staffList = new ArrayList<>();
        // שולפים רק את המנקים!
        String query = "SELECT staff_id, first_name, last_name, role FROM Staff WHERE role = 'Housekeeping'";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                staffList.add(new Staff(
                        rs.getInt("staff_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return staffList;
    }
}