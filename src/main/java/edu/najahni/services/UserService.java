package edu.najahni.services;

import edu.najahni.tools.MyBD;
import java.sql.*;

/**
 * Service pour récupérer les infos utilisateur (email, nom)
 * depuis la table user — utilisé pour les notifications
 */
public class UserService {

    private Connection conn;

    public UserService() {
        conn = MyBD.getInstance().getConn();
    }

    /**
     * Récupère l'email d'un utilisateur par son ID
     */
    public String getEmailById(int userId) throws SQLException {
        String sql = "SELECT email FROM user WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getString("email");
        return null;
    }

    /**
     * Récupère le nom complet d'un utilisateur par son ID
     * Adapte les colonnes selon ta table user (nom, prenom, name, username...)
     */
    public String getNomById(int userId) throws SQLException {
        String sql = "SELECT firstname, lastname FROM user WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            String firstname = rs.getString("firstname");
            String lastname  = rs.getString("lastname");
            if (firstname != null && lastname != null) return firstname + " " + lastname;
            if (firstname != null) return firstname;
            if (lastname != null) return lastname;
        }
        return "Utilisateur #" + userId;
    }
}