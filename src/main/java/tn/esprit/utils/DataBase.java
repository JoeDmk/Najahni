package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBase {
    private static DataBase instance ;
    private final String URL ="jdbc:mysql://127.0.0.1:3306/najahni_db";
    private final String USERNAME="root";
    private final String PASSWORD ="";

    Connection cnx ;


    private DataBase(){

        try {
            cnx = DriverManager.getConnection(URL,USERNAME,PASSWORD);

            System.out.println("DATABASE: Successful");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("DATABASE: Failed");
        }

    }
    public static DataBase getInstance(){
        if (instance == null)
            instance = new DataBase();

        return instance;
    }
    public Connection getCnx(){
        return cnx;
    }

}
