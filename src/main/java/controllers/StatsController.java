package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import models.User;
import services.*;
import util.Type;

/**
 * Statistics controller for admin dashboard.
 */
public class StatsController {

    @FXML private PieChart rolesPieChart;
    @FXML private BarChart<String, Number> statsBarChart;
    @FXML private Label totalLabel;
    @FXML private Label activeLabel;
    @FXML private Label bannedLabel;

    private UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        int entrepreneurs = userService.countByRole(Type.ENTREPRENEUR);
        int mentors = userService.countByRole(Type.MENTOR);
        int investisseurs = userService.countByRole(Type.INVESTISSEUR);
        int admins = userService.countByRole(Type.ADMIN);
        int total = userService.countTotal();
        int active = userService.countActive();
        int banned = userService.countBanned();

        if (totalLabel != null) totalLabel.setText(String.valueOf(total));
        if (activeLabel != null) activeLabel.setText(String.valueOf(active));
        if (bannedLabel != null) bannedLabel.setText(String.valueOf(banned));

        // Pie chart
        if (rolesPieChart != null) {
            rolesPieChart.getData().clear();
            rolesPieChart.getData().add(new PieChart.Data("Entrepreneurs (" + entrepreneurs + ")", entrepreneurs));
            rolesPieChart.getData().add(new PieChart.Data("Mentors (" + mentors + ")", mentors));
            rolesPieChart.getData().add(new PieChart.Data("Investisseurs (" + investisseurs + ")", investisseurs));
            rolesPieChart.getData().add(new PieChart.Data("Admins (" + admins + ")", admins));
        }

        // Bar chart
        if (statsBarChart != null) {
            statsBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Utilisateurs");
            series.getData().add(new XYChart.Data<>("Total", total));
            series.getData().add(new XYChart.Data<>("Actifs", active));
            series.getData().add(new XYChart.Data<>("Bannis", banned));
            series.getData().add(new XYChart.Data<>("Entrepreneurs", entrepreneurs));
            series.getData().add(new XYChart.Data<>("Mentors", mentors));
            series.getData().add(new XYChart.Data<>("Investisseurs", investisseurs));
            statsBarChart.getData().add(series);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();
            DashboardController ctrl = loader.getController();
            User admin = SessionService.getInstance().getCurrentUser();
            ctrl.setCurrentUser(admin);
            Stage stage = (Stage) rolesPieChart.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
