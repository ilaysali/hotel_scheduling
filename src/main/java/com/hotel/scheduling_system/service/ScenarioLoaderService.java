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
import java.io.InputStream;
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

    private void clearDatabase(Connection conn) throws Exception {
        logger.info("Clearing old database tables...");
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            statement.executeUpdate("TRUNCATE TABLE Reservation_Rooms");
            statement.executeUpdate("TRUNCATE TABLE Payments");
            statement.executeUpdate("TRUNCATE TABLE Reservations");
            statement.executeUpdate("TRUNCATE TABLE Housekeeping_Schedule");
            statement.executeUpdate("TRUNCATE TABLE Guests");
            statement.executeUpdate("TRUNCATE TABLE Staff");
            statement.executeUpdate("TRUNCATE TABLE Rooms");
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void insertScenarioData(Connection conn, ScenarioDTO scenario) throws Exception {
        logger.info("Inserting new scenario data into the database...");

        // 1. Insert Rooms
        String insertRoomSql = "INSERT INTO Rooms (room_id, room_number, room_type, room_view) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertRoomSql)) {
            for (RoomDTO room : scenario.rooms()) {
                statement.setInt(1, room.id());
                statement.setInt(2, room.number());
                statement.setString(3, room.type());
                statement.setString(4, room.view());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        // 2. Insert Guests
        String insertGuestSql = "INSERT INTO Guests (guest_id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(insertGuestSql)) {
            for (GuestDTO guest : scenario.guests()) {
                statement.setInt(1, guest.id());
                statement.setString(2, guest.firstName());
                statement.setString(3, guest.lastName());
                statement.setString(4, guest.email());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        // 3. Insert Reservations and Reservation_Rooms
        String insertResSql = "INSERT INTO Reservations (reservation_id, guest_id, preferred_view, room_type) VALUES (?, ?, ?, ?)";
        String insertResRoomsSql = "INSERT INTO Reservation_Rooms (reservation_id, room_id, start_date, end_date) VALUES (?, ?, ?, ?)";

        try (PreparedStatement resStmt = conn.prepareStatement(insertResSql);
             PreparedStatement resRoomsStmt = conn.prepareStatement(insertResRoomsSql)) {

            // Calculate room IDs per room type to avoid O(N*M) complexity in the loop
            java.util.Map<String, Integer> defaultRoomIdsByType = new java.util.HashMap<>();
            int absoluteFallbackRoomId = -1;

            // Build the map
            if (!scenario.rooms().isEmpty()) {
                absoluteFallbackRoomId = scenario.rooms().get(0).id();
                for (RoomDTO room : scenario.rooms()) {
                    // putIfAbsent ensures we only keep the FIRST room ID of each type we encounter
                    defaultRoomIdsByType.putIfAbsent(room.type(), room.id());
                }
            }

            for (ReservationDTO res : scenario.reservations()) {
                // Insert main reservation
                resStmt.setInt(1, res.id());
                resStmt.setInt(2, res.guestId());
                resStmt.setString(3, res.preferredView());
                resStmt.setString(4, res.roomType());
                resStmt.addBatch();

                // Get the dummy room ID from the pre-calculated map in O(1) time
                int dummyRoomId = defaultRoomIdsByType.getOrDefault(res.roomType(), absoluteFallbackRoomId);

                // Assign the valid dummy room ID
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

    /**
     * Shared logic to process the parsed scenario data and save it to the database
     * using a single transaction.
     * @param scenario The parsed ScenarioDTO object
     * @throws Exception If any database operation fails
     */
    private void processAndSaveScenario(ScenarioDTO scenario) throws Exception {
        try (Connection conn = getConnection()) {
            // Disable auto-commit to start a single large transaction
            conn.setAutoCommit(false);
            try {
                clearDatabase(conn);
                insertScenarioData(conn, scenario);

                // Commit all changes at once if everything was successful
                conn.commit();

                logger.info("Successfully loaded scenario: {} rooms, {} guests, {} reservations",
                        scenario.rooms().size(), scenario.guests().size(), scenario.reservations().size());
            } catch (Exception e) {
                // Rollback all changes if any error occurs to maintain data integrity
                conn.rollback();
                throw e;
            } finally {
                // Restore default auto-commit behavior
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Loads a scenario from a given JSON file, clears the database, and inserts the new data.
     * @param jsonFile The JSON file containing the scenario data.
     */
    public void loadScenarioFromFile(File jsonFile) {
        try {
            logger.info("Parsing JSON scenario from file: {}", jsonFile.getName());
            ScenarioDTO scenario = objectMapper.readValue(jsonFile, ScenarioDTO.class);
            processAndSaveScenario(scenario);
        } catch (Exception e) {
            logger.error("Failed to load scenario from file: {}", jsonFile.getName(), e);
            throw new RuntimeException("Could not load scenario from file", e);
        }
    }

    /**
     * Loads a scenario directly from an InputStream.
     * @param inputStream The input stream containing the JSON data.
     */
    public void loadScenarioFromStream(InputStream inputStream) {
        try {
            logger.info("Parsing JSON scenario from stream");
            ScenarioDTO scenario = objectMapper.readValue(inputStream, ScenarioDTO.class);
            processAndSaveScenario(scenario);
        } catch (Exception e) {
            logger.error("Failed to load scenario from stream", e);
            throw new RuntimeException("Could not load scenario from stream", e);
        }
    }
}