package com.najahni.controllers;

import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.models.Role;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.List;

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
