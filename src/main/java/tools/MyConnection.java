package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection shared by ALL modules.
 * <p>
 * Every service class should obtain the connection through:
 * <pre>
 *     Connection cnx = MyConnection.getInstance().getConnection();
 * </pre>
 * Aliases kept for compatibility with code migrated from other branches:
 * <ul>
 *     <li>{@link #getCnx()}  – used by mentorat module</li>
 *     <li>{@link #getConn()} – used by projets / community modules</li>
 * </ul>
 */
public class MyConnection {

    private static final String URL  = "jdbc:mysql://localhost:3306/najahni_db?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PWD  = "";

    private Connection connection;
    private static MyConnection instance;

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PWD);
            System.out.println("Connected to najahni_db successfully!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    /** Primary getter – used by gestion-users, investissement, apprentissage */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PWD);
                System.out.println("Database reconnected.");
            }
        } catch (SQLException e) {
            System.err.println("Reconnection failed: " + e.getMessage());
        }
        return connection;
    }

    /** Alias for mentorat module (DataBase.getCnx) */
    public Connection getCnx() {
        return getConnection();
    }

    /** Alias for projets / community modules (MyBD.getConn) */
    public Connection getConn() {
        return getConnection();
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    /** Close the connection (call on application shutdown). */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
