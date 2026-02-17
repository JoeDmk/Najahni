package edu.najahni.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import edu.najahni.tools.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SignInController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private Connection cnx = MyBD.getInstance().getConn();

    @FXML
    private void handleLogin() {
        String login = txtEmail.getText();
        String password = txtPassword.getText();

        if (login.isEmpty() || password.isEmpty()) {
            lblError.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            String sql = "SELECT * FROM user WHERE (email=? OR id=?) AND password=?";
            PreparedStatement pst = cnx.prepareStatement(sql);
            pst.setString(1, login);
            pst.setString(2, login);
            pst.setString(3, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                int userId = rs.getInt("id");

                Stage stage = (Stage) txtEmail.getScene().getWindow();

                if ("ADMIN".equals(role)) {
                    ouvrirScene(stage, "/DashboardProjets.fxml", "Dashboard Admin - NAJAHNI", null);
                } else {
                    ouvrirScene(stage, "/ClientDashboard.fxml", "Espace Client - NAJAHNI", userId);
                }
            } else {
                lblError.setText("Email / mot de passe incorrect.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Erreur de connexion.");
        }
    }

    private void ouvrirScene(Stage stage, String fxml, String title, Integer userId) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        if (userId != null) {
            ClientDashboardController controller = loader.getController();
            controller.setConnectedUser(userId);
        }

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getMinX());
        stage.setY(screen.getMinY());
        stage.setWidth(screen.getWidth());
        stage.setHeight(screen.getHeight());

        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMaximized(true);
    }
}
