package com.hotel.scheduling_system.database;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationDAO extends BaseDAO { // Inherits database connection logic from BaseDAO

    private static final Logger logger = LoggerFactory.getLogger(ReservationDAO.class);

    /**
     * Retrieves all reservations from the database, joining with Rooms and Guests
     * to provide a complete overview for the scheduling system.
     */
    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();

        // Updated SQL query to include the preferred_view column
        String query = """
                SELECT
                    r.reservation_id AS id,
                    CONCAT(g.first_name, ' ', g.last_name) AS guest_name,
                    rr.start_date,
                    rr.end_date,
                    rm.room_type,
                    r.preferred_view
                FROM Reservations r
                JOIN Reservation_Rooms rr ON r.reservation_id = rr.reservation_id
                JOIN Rooms rm ON rr.room_id = rm.room_id
                JOIN Guests g ON r.guest_id = g.guest_id
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Using the updated Reservation constructor that accepts the preferred view
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getString("guest_name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        RoomType.valueOf(rs.getString("room_type").toUpperCase()),
                        rs.getString("preferred_view")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve all reservations from the database", e);
            throw new RuntimeException("Database error while fetching reservations", e);
        }
        return reservations;
    }

    /**
     * Updates the assigned room for a specific reservation.
     * Used when the scheduling algorithm or user moves a reservation to a new room.
     */
    public void updateReservationRoom(int reservationId, int newRoomId) {
        String query = "UPDATE Reservation_Rooms SET room_id = ? WHERE reservation_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, newRoomId);
            preparedStatement.setInt(2, reservationId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update room for reservation ID: {}", reservationId, e);
            throw new RuntimeException("Database error while updating reservation room", e);
        }
    }

    /**
     * Adds a new reservation to the database.
     * This process handles ID generation and assigns a dummy room based on the requested type
     * so the scheduling algorithm can process it later.
     */
    public void addNewReservation(int guestId, String roomType, LocalDate startDate, LocalDate endDate, String preferredView) {
        try (Connection conn = getConnection()) {

            // 1. Find the next available reservation ID
            int nextResId = 1;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(reservation_id), 0) + 1 AS next_id FROM Reservations")) {
                if (rs.next()) nextResId = rs.getInt("next_id");
            }

            // 2. Find a dummy room ID matching the requested room type to initialize the record
            int dummyRoomId = -1;
            try (PreparedStatement preparedStatement = conn.prepareStatement("SELECT room_id FROM Rooms WHERE room_type = ? LIMIT 1")) {
                preparedStatement.setString(1, roomType);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) dummyRoomId = rs.getInt("room_id");
                }
            }

            // 3. Save entry in the main Reservations table (Includes the preferred_view now)
            try (PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO Reservations (reservation_id, guest_id, preferred_view) VALUES (?, ?, ?)")) {
                preparedStatement.setInt(1, nextResId);
                preparedStatement.setInt(2, guestId);
                preparedStatement.setString(3, preferredView);
                preparedStatement.executeUpdate();
            }

            // 4. Save entry in the Reservation_Rooms table (Dates and room requirements)
            try (PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES (?, ?, ?, ?)")) {
                preparedStatement.setInt(1, nextResId);
                preparedStatement.setInt(2, dummyRoomId);
                preparedStatement.setDate(3, java.sql.Date.valueOf(startDate));
                preparedStatement.setDate(4, java.sql.Date.valueOf(endDate));
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to add new reservation for guest ID: {}", guestId, e);
            throw new RuntimeException("Database error while adding new reservation", e);
        }
    }
}