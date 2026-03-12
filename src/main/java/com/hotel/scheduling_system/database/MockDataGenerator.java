package com.hotel.scheduling_system.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;

@Component
public class MockDataGenerator extends BaseDAO {

    private static final Logger logger = LoggerFactory.getLogger(MockDataGenerator.class);

    // Added a Random instance to randomize view preferences for guests
    private final Random random = new Random();
    private final String[] views = {"Sea", "Pool", "City", "Garden"};

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

            // 1. Define 20 Rooms with different types and views (Price is removed, handled by Enum)
            stmt.executeUpdate("INSERT INTO Rooms (room_id, room_number, room_type, room_view) VALUES " +
                    "(1, 101, 'SINGLE', 'City'), (2, 102, 'SINGLE', 'City'), (3, 103, 'SINGLE', 'Garden'), (4, 104, 'SINGLE', 'Garden'), (5, 105, 'SINGLE', 'Pool'), " +
                    "(6, 201, 'DOUBLE', 'Sea'), (7, 202, 'DOUBLE', 'Sea'), (8, 203, 'DOUBLE', 'Pool'), (9, 204, 'DOUBLE', 'Pool'), (10, 205, 'DOUBLE', 'City'), " +
                    "(11, 206, 'DOUBLE', 'City'), (12, 207, 'DOUBLE', 'Garden'), (13, 208, 'DOUBLE', 'Garden'), (14, 209, 'DOUBLE', 'Sea'), (15, 210, 'DOUBLE', 'Sea'), " +
                    "(16, 301, 'SUITE', 'Sea'), (17, 302, 'SUITE', 'Sea'), (18, 303, 'SUITE', 'Pool'), (19, 304, 'SUITE', 'City'), (20, 305, 'SUITE', 'Garden')");

            // 2. Create 100 Mock Guests
            stmt.executeUpdate(generateGuestsSql());

            // 3. Create 100 Reservation "Envelopes" with view preferences
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


    private String generateGuestsSql() {
        StringBuilder guestsSql = new StringBuilder("INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES ");
        for (int i = 1; i <= 100; i++) {
            guestsSql.append(String.format("(%d, 'Guest%d', 'Last%d', 'user%d@test.com')", i, i, i, i));
            if (i < 100) guestsSql.append(", ");
        }
        return guestsSql.toString();
    }

    private String generateReservationsSql() {
        StringBuilder resSql = new StringBuilder("INSERT INTO Reservations (reservation_id, guest_id, preferred_view) VALUES ");
        for (int i = 1; i <= 100; i++) {
            // Assign a random view, with a 20% chance of no preference (NULL)
            String preferredView = (random.nextInt(5) == 0) ? "NULL" : "'" + views[random.nextInt(views.length)] + "'";
            resSql.append(String.format("(%d, %d, %s)", i, i, preferredView));
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