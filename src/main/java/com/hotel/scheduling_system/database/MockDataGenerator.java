package com.hotel.scheduling_system.database;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
public class MockDataGenerator {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    public void resetAndGenerate() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("Clearing old data...");
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("TRUNCATE TABLE Reservation_Rooms");
            stmt.executeUpdate("TRUNCATE TABLE Payments");
            stmt.executeUpdate("TRUNCATE TABLE Reservations");
            stmt.executeUpdate("TRUNCATE TABLE Housekeeping_Schedule");
            stmt.executeUpdate("TRUNCATE TABLE Guests");
            stmt.executeUpdate("TRUNCATE TABLE Staff");
            stmt.executeUpdate("TRUNCATE TABLE Rooms");
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Inserting Massive Mock Data Scenario...");

            // 1. יצירת 12 חדרים למלון
            stmt.executeUpdate("INSERT INTO Rooms (room_id, room_number, room_type, price_per_night) VALUES " +
                    "(1, 101, 'SINGLE', 100), (2, 102, 'SINGLE', 100), (3, 103, 'SINGLE', 100), (4, 104, 'SINGLE', 100), (5, 105, 'SINGLE', 100), " +
                    "(6, 201, 'DOUBLE', 150), (7, 202, 'DOUBLE', 150), (8, 203, 'DOUBLE', 150), (9, 204, 'DOUBLE', 150), (10, 205, 'DOUBLE', 150), " +
                    "(11, 301, 'SUITE', 300), (12, 302, 'SUITE', 300)");

            // 2. יצירת 15 אורחים
            stmt.executeUpdate("INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES " +
                    "(1, 'John', 'Doe', 'g1@test.com'), (2, 'Jane', 'Smith', 'g2@test.com'), " +
                    "(3, 'Michael', 'Jordan', 'g3@test.com'), (4, 'Serena', 'Williams', 'g4@test.com'), " +
                    "(5, 'Elon', 'Musk', 'g5@test.com'), (6, 'Bill', 'Gates', 'g6@test.com'), " +
                    "(7, 'Steve', 'Jobs', 'g7@test.com'), (8, 'Jeff', 'Bezos', 'g8@test.com'), " +
                    "(9, 'Mark', 'Zuck', 'g9@test.com'), (10, 'Larry', 'Page', 'g10@test.com'), " +
                    "(11, 'Tim', 'Cook', 'g11@test.com'), (12, 'Satya', 'Nadella', 'g12@test.com'), " +
                    "(13, 'Sundar', 'Pichai', 'g13@test.com'), (14, 'Lisa', 'Su', 'g14@test.com'), " +
                    "(15, 'Jensen', 'Huang', 'g15@test.com')");

            // 3. הזמנות (מעטפות)
            stmt.executeUpdate("INSERT INTO Reservations (reservation_id, guest_id) VALUES " +
                    "(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), " +
                    "(11, 11), (12, 12), (13, 13), (14, 14), (15, 15)");

            // 4. תאריכים וסוגי חדרים מבוקשים - כאן מתחיל הכאוס!
            stmt.executeUpdate("INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES " +
                    // --- מלחמת הסוויטות: 4 הזמנות חופפות, רק 2 סוויטות קיימות (חדרים 11,12) ---
                    "(1, 11, '2026-04-10', '2026-04-15'), " +
                    "(2, 11, '2026-04-11', '2026-04-14'), " +
                    "(3, 12, '2026-04-09', '2026-04-16'), " +
                    "(4, 12, '2026-04-10', '2026-04-12'), " +

                    // --- עומס זוגות: 7 הזמנות חופפות בטירוף, רק 5 חדרים זוגיים קיימים (חדרים 6-10) ---
                    "(5, 6, '2026-04-05', '2026-04-08'), " +
                    "(6, 6, '2026-04-05', '2026-04-09'), " +
                    "(7, 7, '2026-04-06', '2026-04-10'), " +
                    "(8, 8, '2026-04-04', '2026-04-07'), " +
                    "(9, 9, '2026-04-05', '2026-04-08'), " +
                    "(10, 10, '2026-04-05', '2026-04-09'), " +
                    "(11, 10, '2026-04-06', '2026-04-08'), " +

                    // --- משחק טטריס (חדרי יחיד): אפשר לסדר אותם יפה אם האלגוריתם חכם (חדרים 1-5) ---
                    "(12, 1, '2026-04-01', '2026-04-05'), " +
                    "(13, 1, '2026-04-05', '2026-04-10'), " + // מתחיל בדיוק מתי ש-12 עוזב!
                    "(14, 2, '2026-04-02', '2026-04-07'), " +
                    "(15, 3, '2026-04-15', '2026-04-20')");

            System.out.println("Massive Mock Data Generation Complete!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}