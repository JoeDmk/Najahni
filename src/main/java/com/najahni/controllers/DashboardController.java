package com.najahni.controllers;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.models.Role;
import com.najahni.services.DeadlineService;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.services.ProjectService;
import com.najahni.services.UserService;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.WrappedTextCellFactory;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for the Dashboard view.
 * Displays platform statistics and recent activity with smooth animations.
 */
public class DashboardController {

    @FXML
    private Label lblTotalUsers;

    @FXML
    private Label lblEntrepreneurs;

    @FXML
    private Label lblInvestors;

    @FXML
    private Label lblTotalProjects;

    @FXML
    private Label lblApprovedProjects;

    @FXML
    private Label lblPendingProjects;

    @FXML
    private Label lblTotalInvestments;

    @FXML
    private Label lblTotalAmount;

    @FXML private Label lblUrgentDeadlines;
    @FXML private Label lblUpcomingDeadlines;

    // ─── Charts ──────────────────────────────────────────────
    @FXML private PieChart chartOpportunityStatus;
    @FXML private BarChart<String, Number> chartProjectAmounts;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    @FXML
    private TableView<Project> recentProjectsTable;

    @FXML
    private TableColumn<Project, String> colProjectTitle;

    @FXML
    private TableColumn<Project, String> colProjectSector;

    @FXML
    private TableColumn<Project, ProjectStatus> colProjectStatus;

    @FXML
    private TableColumn<Project, String> colProjectEntrepreneur;

    private final UserService userService;
    private final ProjectService projectService;
    private final InvestmentOpportunityService investmentService;

    public DashboardController() {
        this.userService = new UserService();
        this.projectService = new ProjectService();
        this.investmentService = new InvestmentOpportunityService();
    }

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        refreshDashboard();
        
        // Animate stat cards on load
        animateStatCards();
    }

    /**
     * Animates the stat cards with a staggered fade-in effect.
     */
    private void animateStatCards() {
        // Get parent containers for stat cards
        if (lblTotalUsers.getParent() != null) {
            Node userCard = lblTotalUsers.getParent();
            Node projectCard = lblTotalProjects.getParent();
            Node investmentCard = lblTotalInvestments.getParent();
            
            animateCard(userCard, 0);
            animateCard(projectCard, 100);
            animateCard(investmentCard, 200);
        }
        
        // Animate table
        if (recentProjectsTable != null) {
            animateCard(recentProjectsTable, 300);
        }
    }
    
    /**
     * Animates a single card with fade and scale effect.
     */
    private void animateCard(Node card, int delayMs) {
        if (card == null) return;
        
        card.setOpacity(0);
        card.setScaleX(0.95);
        card.setScaleY(0.95);
        
        FadeTransition fade = new FadeTransition(Duration.millis(400), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMs));
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), card);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setDelay(Duration.millis(delayMs));
        
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.play();
    }

    /**
     * Sets up table columns.
     */
    private void setupTableColumns() {
        colProjectTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colProjectSector.setCellValueFactory(new PropertyValueFactory<>("sector"));
        colProjectStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProjectEntrepreneur.setCellValueFactory(new PropertyValueFactory<>("entrepreneurName"));
        
        // Apply text wrapping to prevent truncation
        colProjectTitle.setCellFactory(new WrappedTextCellFactory<>());
        colProjectSector.setCellFactory(new WrappedTextCellFactory<>());
        colProjectEntrepreneur.setCellFactory(new WrappedTextCellFactory<>());
    }

    /**
     * Refreshes all dashboard data.
     */
    @FXML
    public void refreshDashboard() {
        loadUserStats();
        loadProjectStats();
        loadInvestmentStats();
        loadDeadlineStats();
        loadCharts();
        loadRecentProjects();
    }

    /**
     * Loads user statistics.
     */
    private void loadUserStats() {
        int totalUsers = userService.findAll().size();
        int entrepreneurs = userService.findByRole(Role.ENTREPRENEUR).size();
        int investors = userService.findByRole(Role.INVESTOR).size();

        lblTotalUsers.setText(String.valueOf(totalUsers));
        lblEntrepreneurs.setText("Entrepreneurs : " + entrepreneurs);
        lblInvestors.setText("Investisseurs : " + investors);
    }

    /**
     * Loads project statistics.
     */
    private void loadProjectStats() {
        int totalProjects = projectService.findAll().size();
        int approvedProjects = projectService.countByStatus(ProjectStatus.APPROVED);
        int pendingProjects = projectService.countByStatus(ProjectStatus.PENDING);

        lblTotalProjects.setText(String.valueOf(totalProjects));
        lblApprovedProjects.setText("Approuvés : " + approvedProjects);
        lblPendingProjects.setText("En attente : " + pendingProjects);
    }

    /**
     * Loads investment statistics.
     */
    private void loadInvestmentStats() {
        int totalInvestments = investmentService.findAll().size();
        BigDecimal totalAmount = investmentService.getTotalTargetAmount();

        lblTotalInvestments.setText(String.valueOf(totalInvestments));
        lblTotalAmount.setText(String.format("Total : %,.2f €", totalAmount));
    }

    /**
     * Loads recent projects into the table.
     */
    private void loadRecentProjects() {
        List<Project> projects = projectService.findAll();
        // Show only the 10 most recent projects
        if (projects.size() > 10) {
            projects = projects.subList(0, 10);
        }
        recentProjectsTable.setItems(FXCollections.observableArrayList(projects));
    }

    // ─── DEADLINE STATS ──────────────────────────────────────

    private void loadDeadlineStats() {
        if (lblUrgentDeadlines == null) return;

        List<InvestmentOpportunity> all = investmentService.findAll();
        long urgent = all.stream()
            .filter(o -> o.getDeadline() != null)
            .filter(o -> {
                DeadlineService.DeadlineLevel level = DeadlineService.getLevel(o.getDeadline());
                return level == DeadlineService.DeadlineLevel.URGENT || level == DeadlineService.DeadlineLevel.EXPIRED;
            })
            .count();
        long upcoming = all.stream()
            .filter(o -> o.getDeadline() != null)
            .filter(o -> {
                DeadlineService.DeadlineLevel level = DeadlineService.getLevel(o.getDeadline());
                return level == DeadlineService.DeadlineLevel.ATTENTION || level == DeadlineService.DeadlineLevel.PROCHE;
            })
            .count();

        lblUrgentDeadlines.setText(String.valueOf(urgent));
        lblUpcomingDeadlines.setText("À venir : " + upcoming);
    }

    // ─── CHARTS ──────────────────────────────────────────────

    private void loadCharts() {
        loadOpportunityStatusChart();
        loadProjectAmountsChart();
    }

    /**
     * PieChart showing opportunity status distribution (OPEN / CLOSED / FUNDED).
     */
    private void loadOpportunityStatusChart() {
        if (chartOpportunityStatus == null) return;

        int open = investmentService.countByStatus(OpportunityStatus.OPEN);
        int closed = investmentService.countByStatus(OpportunityStatus.CLOSED);
        int funded = investmentService.countByStatus(OpportunityStatus.FUNDED);

        chartOpportunityStatus.setData(FXCollections.observableArrayList(
            new PieChart.Data("Ouvertes (" + open + ")", open),
            new PieChart.Data("Fermées (" + closed + ")", closed),
            new PieChart.Data("Financées (" + funded + ")", funded)
        ));

        // Color coding after data is added
        chartOpportunityStatus.getData().forEach(data -> {
            String name = data.getName();
            String color;
            if (name.startsWith("Ouvertes"))   color = "#27ae60";
            else if (name.startsWith("Fermées")) color = "#e74c3c";
            else                                  color = "#3498db";
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        });
    }

    /**
     * BarChart showing top 5 projects by total target investment amount.
     */
    private void loadProjectAmountsChart() {
        if (chartProjectAmounts == null) return;

        List<InvestmentOpportunity> allOpps = investmentService.findAll();

        // Sum target amounts by projectTitle
        Map<String, Double> amountByProject = allOpps.stream()
            .filter(o -> o.getProjectTitle() != null && o.getTargetAmount() != null)
            .collect(Collectors.groupingBy(
                o -> o.getProjectTitle().length() > 18
                    ? o.getProjectTitle().substring(0, 15) + "..."
                    : o.getProjectTitle(),
                Collectors.summingDouble(o -> o.getTargetAmount().doubleValue())
            ));

        // Sort descending by amount and take top 5
        Map<String, Double> top5 = amountByProject.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Montant");
        top5.forEach((project, amount) ->
            series.getData().add(new XYChart.Data<>(project, amount)));

        chartProjectAmounts.getData().clear();
        chartProjectAmounts.getData().add(series);

        // Color bars after rendering
        series.getData().forEach(d -> {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: #0f3460;");
            }
        });
    }

    /**
     * Opens the add user form.
     */
    @FXML
    public void addNewUser() {
        AlertUtils.showInfo("Ajouter Utilisateur", "Veuillez accéder à la section Utilisateurs pour ajouter un nouvel utilisateur.");
    }

    /**
     * Opens the add project form.
     */
    @FXML
    public void addNewProject() {
        AlertUtils.showInfo("Ajouter Projet", "Veuillez accéder à la section Projets pour créer un nouveau projet.");
    }

    /**
     * Opens the add investment form.
     */
    @FXML
    public void addNewInvestment() {
        AlertUtils.showInfo("Ajouter Investissement", "Veuillez accéder à la section Investissements pour ajouter un nouvel investissement.");
    }
}
