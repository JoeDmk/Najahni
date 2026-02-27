package com.najahni.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class for database connection management.
 * Ensures only one database connection instance exists throughout the application.
 */
public class DBConnection {

    // Database configuration
    private static final String URL = "jdbc:mysql://localhost:3306/najahni_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Singleton instance
    private static DBConnection instance;
    private Connection connection;

    /**
     * Private constructor to prevent external instantiation.
     * Establishes database connection.
     */
    private DBConnection() {
        try {
            // Driver auto-registered via SPI in modern JDBC
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Database connection established successfully!");
            
        } catch (SQLException e) {
            System.err.println("✗ Failed to connect to database!");
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of DBConnection.
     * Creates new instance if none exists.
     * 
     * @return DBConnection singleton instance
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Returns the active database connection.
     * Reconnects if connection is closed.
     * 
     * @return Connection object
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Reconnect if connection is lost
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✓ Database reconnected successfully!");
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to reconnect to database!");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error closing database connection!");
            e.printStackTrace();
        }
    }
}
