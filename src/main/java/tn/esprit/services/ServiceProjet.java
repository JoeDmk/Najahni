package tn.esprit.services;

import tn.esprit.models.Projet;
import tn.esprit.utils.DataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProjet {
    private Connection cnx;

    public ServiceProjet() {
        cnx = DataBase.getInstance().getCnx();
    }

    public List<Projet> getAll() {
        List<Projet> list = new ArrayList<>();
        // Expects: id, titre, description
        String qry = "SELECT * FROM `projet`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                Projet p = new Projet();
                p.setId(rs.getInt("id"));
                p.setTitre(rs.getString("titre"));
                p.setDescription(rs.getString("description"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }
}
