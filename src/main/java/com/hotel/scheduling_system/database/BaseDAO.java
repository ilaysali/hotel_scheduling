package com.hotel.scheduling_system.database;

import org.springframework.beans.factory.annotation.Value;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base Data Access Object providing database connection capabilities.
 * Inherits configuration from application.properties.
 */
public abstract class BaseDAO {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String user;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * Establishes a connection to the MySQL database.
     * @return a Connection object
     * @throws SQLException if a database access error occurs
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}