package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.RoomType;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationDAO {
    private final String url = "jdbc:mysql://localhost:3306/hotel_db";
    private final String user = "root";
    private final String password = "admin";

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();

        // עדכנו את השאילתה כדי לעשות JOIN גם לטבלת Guests ולמשוך את השם המלא!
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
                        rs.getString("guest_name"), // קוראים את השם החדש מה-DB
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        RoomType.valueOf(rs.getString("room_type"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    public void updateReservationRoom(int reservationId, int newRoomId) {
        String query = "UPDATE Reservation_Rooms SET room_id = ? WHERE reservation_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, newRoomId);
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}