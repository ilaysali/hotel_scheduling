package com.hotel.scheduling_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotel.scheduling_system.database.BaseDAO;
import com.hotel.scheduling_system.dto.GuestDTO;
import com.hotel.scheduling_system.dto.ReservationDTO;
import com.hotel.scheduling_system.dto.RoomDTO;
import com.hotel.scheduling_system.dto.ScenarioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class ScenarioLoaderService extends BaseDAO {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioLoaderService.class);
    private final ObjectMapper objectMapper;

    public ScenarioLoaderService() {
        this.objectMapper = new ObjectMapper();
        // Register module to properly parse LocalDate from JSON strings like "2026-04-10"
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Loads a scenario from a given JSON file, clears the database, and inserts the new data.
     * @param jsonFile The JSON file containing the scenario data.
     */
    public void loadScenarioFromFile(File jsonFile) {
        try {
            logger.info("Parsing JSON scenario from file: {}", jsonFile.getName());
            ScenarioDTO scenario = objectMapper.readValue(jsonFile, ScenarioDTO.class);

            try (Connection conn = getConnection()) {
                clearDatabase(conn);
                insertScenarioData(conn, scenario);
                logger.info("Successfully loaded scenario: {} rooms, {} guests, {} reservations",
                        scenario.rooms().size(), scenario.guests().size(), scenario.reservations().size());
            }

        } catch (Exception e) {
            logger.error("Failed to load scenario from file: {}", jsonFile.getName(), e);
            throw new RuntimeException("Could not load scenario", e);
        }
    }

    private void clearDatabase(Connection conn) throws Exception {
        logger.info("Clearing old database tables...");
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("TRUNCATE TABLE Reservation_Rooms");
            stmt.executeUpdate("TRUNCATE TABLE Payments");
            stmt.executeUpdate("TRUNCATE TABLE Reservations");
            stmt.executeUpdate("TRUNCATE TABLE Housekeeping_Schedule");
            stmt.executeUpdate("TRUNCATE TABLE Guests");
            stmt.executeUpdate("TRUNCATE TABLE Staff");
            stmt.executeUpdate("TRUNCATE TABLE Rooms");
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void insertScenarioData(Connection conn, ScenarioDTO scenario) throws Exception {
        logger.info("Inserting new scenario data into the database...");

        // 1. Insert Rooms
        String insertRoomSql = "INSERT INTO Rooms (room_id, room_number, room_type, room_view) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertRoomSql)) {
            for (RoomDTO room : scenario.rooms()) {
                pstmt.setInt(1, room.id());
                pstmt.setInt(2, room.number());
                pstmt.setString(3, room.type());
                pstmt.setString(4, room.view());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // 2. Insert Guests
        String insertGuestSql = "INSERT INTO Guests (guest_id, first_name, last_name) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertGuestSql)) {
            for (GuestDTO guest : scenario.guests()) {
                pstmt.setInt(1, guest.id());
                pstmt.setString(2, guest.firstName());
                pstmt.setString(3, guest.lastName());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // 3. Insert Reservations and Reservation_Rooms
        // Added room_type to the SQL query and values
        String insertResSql = "INSERT INTO Reservations (reservation_id, guest_id, preferred_view, room_type) VALUES (?, ?, ?, ?)";
        String insertResRoomsSql = "INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES (?, ?, ?, ?)";

        try (PreparedStatement resStmt = conn.prepareStatement(insertResSql);
             PreparedStatement resRoomsStmt = conn.prepareStatement(insertResRoomsSql)) {

            for (ReservationDTO res : scenario.reservations()) {
                // Insert main reservation with all 4 parameters
                resStmt.setInt(1, res.id());
                resStmt.setInt(2, res.guestId());
                resStmt.setString(3, res.preferredView());
                resStmt.setString(4, res.roomType());
                resStmt.addBatch();

                // Find a valid room ID from the loaded scenario to satisfy the Foreign Key constraint.
                // It tries to find a room matching the requested type, or defaults to the first available room.
                int dummyRoomId = scenario.rooms().stream()
                        .filter(r -> r.type().equals(res.roomType()))
                        .findFirst()
                        .map(com.hotel.scheduling_system.dto.RoomDTO::id)
                        .orElse(scenario.rooms().get(0).id());

                // Assign the valid dummy room ID instead of -1
                resRoomsStmt.setInt(1, res.id());
                resRoomsStmt.setInt(2, dummyRoomId);
                resRoomsStmt.setDate(3, java.sql.Date.valueOf(res.startDate()));
                resRoomsStmt.setDate(4, java.sql.Date.valueOf(res.endDate()));
                resRoomsStmt.addBatch();
            }
            resStmt.executeBatch();
            resRoomsStmt.executeBatch();
        }
    }
    // Load scenario directly from an InputStream (safe for classpath resources)
    public void loadScenarioFromStream(java.io.InputStream inputStream) {
        try {
            logger.info("Parsing JSON scenario from stream");
            ScenarioDTO scenario = objectMapper.readValue(inputStream, ScenarioDTO.class);

            try (Connection conn = getConnection()) {
                clearDatabase(conn);
                insertScenarioData(conn, scenario);
                logger.info("Successfully loaded scenario: {} rooms, {} guests, {} reservations",
                        scenario.rooms().size(), scenario.guests().size(), scenario.reservations().size());
            }

        } catch (Exception e) {
            logger.error("Failed to load scenario from stream", e);
            throw new RuntimeException("Could not load scenario", e);
        }
    }
}