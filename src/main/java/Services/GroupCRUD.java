package Services;

import Entites.Group;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupCRUD implements IntrefaceCRUD<Group> {

    Connection conn;

    public GroupCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(Group g) throws SQLException {

        // ✅ UNICITY CHECK
        if (existsByName(g.getName())) {
            throw new SQLException("GROUP_NAME_EXISTS");
        }

        String req = "INSERT INTO groups (name, description, group_admin_id, is_private) VALUES (?, ?, ?,?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        pst.setString(1, g.getName());
        pst.setString(2, g.getDescription());
        pst.setInt(3, g.getGroupAdminId());
        pst.setBoolean(4, g.getIsPrivate());

        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) g.setId(rs.getInt(1));
    }



    @Override
    public void modifier(Group g) throws SQLException {

        // ✅ UNICITY CHECK (ignore current row)
        if (existsByNameExceptId(g.getName(), g.getId())) {
            throw new SQLException("GROUP_NAME_EXISTS");
        }

        String req = "UPDATE groups SET name=?, description=?, group_admin_id=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);

        pst.setString(1, g.getName());
        pst.setString(2, g.getDescription());
        pst.setInt(3, g.getGroupAdminId());
        pst.setInt(4, g.getId());

        pst.executeUpdate();
    }


    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM groups WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    @Override
    public List<Group> afficher() throws SQLException {

        List<Group> list = new ArrayList<>();
        String req = "SELECT * FROM groups";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            list.add(new Group(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("group_admin_id"),
                    rs.getBoolean("is_private")   // 🔥 ADD THIS
            ));

        }

        return list;
    }
    // ✅ true if name exists (case-insensitive)
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM groups WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ✅ true if name exists for another group (for update)
    public boolean existsByNameExceptId(String name, int id) throws SQLException {
        String sql = "SELECT 1 FROM groups WHERE LOWER(name) = LOWER(?) AND id <> ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


}
