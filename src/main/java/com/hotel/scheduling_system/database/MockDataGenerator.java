package com.hotel.scheduling_system.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class MockDataGenerator extends BaseDAO {

    private static final Logger logger = LoggerFactory.getLogger(MockDataGenerator.class);

    /**
     * Resets the database by clearing all tables and generating a fresh set of mock data.
     * Creates a high-occupancy scenario for April-May 2026.
     */
    public void resetAndGenerate() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            logger.info("Clearing old data...");

            // Temporarily disable foreign key checks to allow TRUNCATE on all tables
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("TRUNCATE TABLE Reservation_Rooms");
            stmt.executeUpdate("TRUNCATE TABLE Payments");
            stmt.executeUpdate("TRUNCATE TABLE Reservations");
            stmt.executeUpdate("TRUNCATE TABLE Housekeeping_Schedule");
            stmt.executeUpdate("TRUNCATE TABLE Guests");
            stmt.executeUpdate("TRUNCATE TABLE Staff");
            stmt.executeUpdate("TRUNCATE TABLE Rooms");
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

            logger.info("Generating Balanced Crowded Scenario: 20 Rooms, 100 Reservations (April-May)...");

            // 1. Define 20 Rooms with different types and prices
            stmt.executeUpdate("INSERT INTO Rooms (room_id, room_number, room_type, price_per_night) VALUES " +
                    "(1, 101, 'SINGLE', 100), (2, 102, 'SINGLE', 100), (3, 103, 'SINGLE', 100), (4, 104, 'SINGLE', 100), (5, 105, 'SINGLE', 100), " +
                    "(6, 201, 'DOUBLE', 150), (7, 202, 'DOUBLE', 150), (8, 203, 'DOUBLE', 150), (9, 204, 'DOUBLE', 150), (10, 205, 'DOUBLE', 150), " +
                    "(11, 206, 'DOUBLE', 150), (12, 207, 'DOUBLE', 150), (13, 208, 'DOUBLE', 150), (14, 209, 'DOUBLE', 150), (15, 210, 'DOUBLE', 150), " +
                    "(16, 301, 'SUITE', 300), (17, 302, 'SUITE', 300), (18, 303, 'SUITE', 300), (19, 304, 'SUITE', 300), (20, 305, 'SUITE', 300)");

            // 2. Create 100 Mock Guests
            stmt.executeUpdate(generateGuestsSql());

            // 3. Create 100 Reservation "Envelopes"
            stmt.executeUpdate(generateReservationsSql());

            // 4. Assign Rooms - Two months (April-May) - Dense but logical scheduling
            stmt.executeUpdate(generateReservationRoomsSql());

            // 5. Staff Members
            stmt.executeUpdate("INSERT INTO Staff (staff_id, first_name, last_name, role) VALUES " +
                    "(1, 'Rosa', 'Diaz', 'Housekeeping'), (2, 'Terry', 'Jeffords', 'Maintenance')");

            logger.info("Done! 20 rooms, 100 reservations generated for April-May. High occupancy but manageable.");

        } catch (Exception e) {
            logger.error("Database error occurred while resetting and generating mock data", e);
            throw new RuntimeException("Failed to generate mock data", e);
        }
    }

    // --- Extracted Helper Methods ---

    private String generateGuestsSql() {
        StringBuilder guestsSql = new StringBuilder("INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES ");
        for (int i = 1; i <= 100; i++) {
            guestsSql.append(String.format("(%d, 'Guest%d', 'Last%d', 'user%d@test.com')", i, i, i, i));
            if (i < 100) guestsSql.append(", ");
        }
        return guestsSql.toString();
    }

    private String generateReservationsSql() {
        StringBuilder resSql = new StringBuilder("INSERT INTO Reservations (reservation_id, guest_id) VALUES ");
        for (int i = 1; i <= 100; i++) {
            resSql.append(String.format("(%d, %d)", i, i));
            if (i < 100) resSql.append(", ");
        }
        return resSql.toString();
    }

    private String generateReservationRoomsSql() {
        StringBuilder resRoomsSql = new StringBuilder("INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES ");
        int resId = 1;

        for (int roomId = 1; roomId <= 20; roomId++) {
            resRoomsSql.append(String.format("(%d, %d, '2026-04-01', '2026-04-07'), ", resId++, roomId));
            resRoomsSql.append(String.format("(%d, %d, '2026-04-07', '2026-04-15'), ", resId++, roomId));
            resRoomsSql.append(String.format("(%d, %d, '2026-05-01', '2026-05-10'), ", resId++, roomId));

            String startDate = (roomId % 5 == 0) ? "2026-05-08" : "2026-05-15";
            resRoomsSql.append(String.format("(%d, %d, '%s', '2026-05-25'), ", resId++, roomId, startDate));

            if (resId > 100) break;
        }

        String finalSql = resRoomsSql.toString().trim();
        if (finalSql.endsWith(",")) {
            finalSql = finalSql.substring(0, finalSql.length() - 1);
        }
        return finalSql;
    }
}