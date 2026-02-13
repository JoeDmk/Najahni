package edu.najahni.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBD {

    private Connection conn;

    private final String URL = "jdbc:mysql://localhost:3306/najahni";
    private final String USER = "root";
    private final String PASS = "";  // Mets ton mot de passe root MySQL si tu en as défini un

    private static MyBD instance;

    private MyBD() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connected to database 'najahni' successfully!");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    public static MyBD getInstance() {
        if (instance == null) {
            instance = new MyBD();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }
}