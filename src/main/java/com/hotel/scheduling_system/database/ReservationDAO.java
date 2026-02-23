package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.RoomType;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String query = """
                SELECT 
                    r.reservation_id AS id, 
                    CONCAT(g.first_name, ' ', g.last_name) AS guest_name,
                    rr.start_date, 
                    rr.end_date, 
                    rm.room_type 
                FROM Reservations r
                JOIN Reservation_Rooms rr ON r.reservation_id = rr.reservation_id
                JOIN Rooms rm ON rr.room_id = rm.room_id
                JOIN Guests g ON r.guest_id = g.guest_id
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getString("guest_name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        RoomType.valueOf(rs.getString("room_type"))
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reservations;
    }

    public void updateReservationRoom(int reservationId, int newRoomId) {
        String query = "UPDATE Reservation_Rooms SET room_id = ? WHERE reservation_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, newRoomId);
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- הפונקציה החדשה שמוסיפה הזמנה למסד הנתונים! ---
    public void addNewReservation(int guestId, String roomType, LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            // 1. מציאת מזהה ההזמנה הבא הפנוי (ID)
            int nextResId = 1;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(reservation_id), 0) + 1 AS next_id FROM Reservations")) {
                if (rs.next()) nextResId = rs.getInt("next_id");
            }

            // 2. מציאת חדר דמה (Dummy) שמתאים לסוג החדר המבוקש כדי להוות כרטיסייה התחלתית לאלגוריתם
            int dummyRoomId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT room_id FROM Rooms WHERE room_type = ? LIMIT 1")) {
                pstmt.setString(1, roomType);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) dummyRoomId = rs.getInt("room_id");
                }
            }

            // 3. שמירה בטבלת Reservations (קשר בין הזמנה לאורח)
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Reservations (reservation_id, guest_id) VALUES (?, ?)")) {
                pstmt.setInt(1, nextResId);
                pstmt.setInt(2, guestId);
                pstmt.executeUpdate();
            }

            // 4. שמירה בטבלת Reservation_Rooms (התאריכים והחדר שביקש)
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES (?, ?, ?, ?)")) {
                pstmt.setInt(1, nextResId);
                pstmt.setInt(2, dummyRoomId);
                pstmt.setDate(3, java.sql.Date.valueOf(startDate));
                pstmt.setDate(4, java.sql.Date.valueOf(endDate));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}