package controllers.investissement;

import models.investissement.InvestmentOffer;
import models.investissement.InvestmentOpportunity;
import models.investissement.OpportunityStatus;
import models.investissement.Project;
import models.investissement.ProjectStatus;
import services.investissement.DeadlineService;
import services.investissement.InvestmentOfferService;
import services.investissement.InvestmentOpportunityService;
import services.investissement.ProjectService;
import services.investissement.CurrencyService;
import tools.MyConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur du tableau de bord Investissement.
 * Affiche les statistiques, graphiques et projets récents.
 */
public class DashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblEntrepreneurs;
    @FXML private Label lblInvestors;
    @FXML private Label lblTotalProjects;
    @FXML private Label lblApprovedProjects;
    @FXML private Label lblPendingProjects;
    @FXML private Label lblTotalInvestments;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblUrgentDeadlines;
    @FXML private Label lblUpcomingDeadlines;
    @FXML private HBox statsContainer;
    @FXML private PieChart chartOpportunityStatus;
    @FXML private BarChart<String, Number> chartProjectAmounts;
    @FXML private TableView<Project> recentProjectsTable;
    @FXML private TableColumn<Project, String> colProjectTitle;
    @FXML private TableColumn<Project, String> colProjectSector;
    @FXML private TableColumn<Project, String> colProjectStatus;
    @FXML private TableColumn<Project, String> colProjectEntrepreneur;

    private final ProjectService projectService = new ProjectService();
    private final InvestmentOpportunityService opportunityService = new InvestmentOpportunityService();
    private final InvestmentOfferService offerService = new InvestmentOfferService();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDashboardData();
    }

    private void setupTableColumns() {
        colProjectTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colProjectSector.setCellValueFactory(new PropertyValueFactory<>("sector"));
        colProjectStatus.setCellValueFactory(cellData -> {
            ProjectStatus status = cellData.getValue().getStatus();
            String display = switch (status) {
                case DRAFT -> "📝 Brouillon";
                case PENDING -> "⏳ En attente";
                case APPROVED -> "✅ Approuvé";
                case REJECTED -> "❌ Rejeté";
                case FUNDED -> "💰 Financé";
            };
            return new javafx.beans.property.SimpleStringProperty(display);
        });
        colProjectEntrepreneur.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getEntrepreneurName();
            return new javafx.beans.property.SimpleStringProperty(
                    name != null ? name : "Entrepreneur #" + cellData.getValue().getEntrepreneurId());
        });
    }

    private void loadDashboardData() {
        try {
            loadUserStats();
            loadProjectStats();
            loadInvestmentStats();
            loadDeadlineStats();
            loadOpportunityChart();
            loadProjectAmountsChart();
            loadRecentProjects();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du dashboard investissement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserStats() {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            String sql = "SELECT COUNT(*) as total, " +
                    "SUM(CASE WHEN role = 'ENTREPRENEUR' THEN 1 ELSE 0 END) as entrepreneurs, " +
                    "SUM(CASE WHEN role = 'INVESTISSEUR' OR role = 'INVESTOR' THEN 1 ELSE 0 END) as investors " +
                    "FROM user";
            try (PreparedStatement stmt = cnx.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lblTotalUsers.setText(String.valueOf(rs.getInt("total")));
                    lblEntrepreneurs.setText("Entrepreneurs : " + rs.getInt("entrepreneurs"));
                    lblInvestors.setText("Investisseurs : " + rs.getInt("investors"));
                }
            }
        } catch (Exception e) {
            lblTotalUsers.setText("--");
            lblEntrepreneurs.setText("Entrepreneurs : --");
            lblInvestors.setText("Investisseurs : --");
        }
    }

    private void loadProjectStats() {
        try {
            List<Project> allProjects = projectService.findAll();
            int approved = projectService.countByStatus(ProjectStatus.APPROVED);
            int pending = projectService.countByStatus(ProjectStatus.PENDING);

            lblTotalProjects.setText(String.valueOf(allProjects.size()));
            lblApprovedProjects.setText("Approuvés : " + approved);
            lblPendingProjects.setText("En attente : " + pending);
        } catch (Exception e) {
            lblTotalProjects.setText("--");
            lblApprovedProjects.setText("Approuvés : --");
            lblPendingProjects.setText("En attente : --");
        }
    }

    private void loadInvestmentStats() {
        try {
            List<InvestmentOffer> allOffers = offerService.findAll();
            BigDecimal totalAmount = opportunityService.getTotalTargetAmount();

            String userCurrency = "EUR";
            try {
                var user = services.SessionService.getInstance().getCurrentUser();
                if (user != null) userCurrency = user.getPreferredCurrency();
            } catch (Exception ignored) {}
            CurrencyService cs = new CurrencyService();
            double converted = cs.convert(
                    (totalAmount != null ? totalAmount : BigDecimal.ZERO).doubleValue(), "EUR", userCurrency);

            lblTotalInvestments.setText(String.valueOf(allOffers.size()));
            lblTotalAmount.setText("Total : " + CurrencyService.format(converted, userCurrency));
        } catch (Exception e) {
            lblTotalInvestments.setText("--");
            lblTotalAmount.setText("Total : --");
        }
    }

    private void loadDeadlineStats() {
        try {
            List<InvestmentOpportunity> opportunities = opportunityService.findAll();
            long urgent = 0;
            long upcoming = 0;

            for (InvestmentOpportunity opp : opportunities) {
                LocalDate deadline = opp.getDeadline();
                if (deadline != null) {
                    DeadlineService.DeadlineLevel level = DeadlineService.getLevel(deadline);
                    if (level == DeadlineService.DeadlineLevel.URGENT || level == DeadlineService.DeadlineLevel.EXPIRED) {
                        urgent++;
                    } else if (level == DeadlineService.DeadlineLevel.ATTENTION || level == DeadlineService.DeadlineLevel.PROCHE) {
                        upcoming++;
                    }
                }
            }

            lblUrgentDeadlines.setText(String.valueOf(urgent));
            lblUpcomingDeadlines.setText("À venir : " + upcoming);
        } catch (Exception e) {
            lblUrgentDeadlines.setText("--");
            lblUpcomingDeadlines.setText("À venir : --");
        }
    }

    private void loadOpportunityChart() {
        try {
            int open = opportunityService.countByStatus(OpportunityStatus.OPEN);
            int closed = opportunityService.countByStatus(OpportunityStatus.CLOSED);
            int funded = opportunityService.countByStatus(OpportunityStatus.FUNDED);

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Ouvertes (" + open + ")", open),
                    new PieChart.Data("Fermées (" + closed + ")", closed),
                    new PieChart.Data("Financées (" + funded + ")", funded)
            );

            chartOpportunityStatus.setData(pieData);
            chartOpportunityStatus.setTitle("Statut des Opportunités");
        } catch (Exception e) {
            System.err.println("Erreur chargement graphique opportunités: " + e.getMessage());
        }
    }

    private void loadProjectAmountsChart() {
        try {
            List<Project> projects = projectService.findAll();
            List<InvestmentOpportunity> allOpps = opportunityService.findAll();

            // Group opportunities by project
            Map<Integer, BigDecimal> projectAmounts = allOpps.stream()
                    .filter(opp -> opp.getTargetAmount() != null)
                    .collect(Collectors.groupingBy(
                            InvestmentOpportunity::getProjectId,
                            Collectors.reducing(BigDecimal.ZERO, InvestmentOpportunity::getTargetAmount, BigDecimal::add)
                    ));

            // Get top 5 projects by amount
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Montant cible");

            projectAmounts.entrySet().stream()
                    .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        String projectTitle = projects.stream()
                                .filter(p -> p.getId() == entry.getKey())
                                .findFirst()
                                .map(Project::getTitle)
                                .orElse("Projet #" + entry.getKey());
                        // Truncate long titles
                        if (projectTitle.length() > 15) {
                            projectTitle = projectTitle.substring(0, 12) + "...";
                        }
                        series.getData().add(new XYChart.Data<>(projectTitle, entry.getValue()));
                    });

            chartProjectAmounts.getData().clear();
            chartProjectAmounts.getData().add(series);
        } catch (Exception e) {
            System.err.println("Erreur chargement graphique montants: " + e.getMessage());
        }
    }

    private void loadRecentProjects() {
        try {
            List<Project> projects = projectService.findAll();

            // Sort by creation date descending, take first 10
            List<Project> recent = projects.stream()
                    .sorted(Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(10)
                    .collect(Collectors.toList());

            recentProjectsTable.setItems(FXCollections.observableArrayList(recent));
        } catch (Exception e) {
            System.err.println("Erreur chargement projets récents: " + e.getMessage());
        }
    }

    @FXML
    void refreshDashboard(ActionEvent event) {
        loadDashboardData();
    }

    @FXML
    void refreshDashboard() {
        loadDashboardData();
    }

    @FXML
    void addNewUser(ActionEvent event) {
        System.out.println("Action: Ajouter utilisateur");
    }

    @FXML
    void addNewProject(ActionEvent event) {
        System.out.println("Action: Créer projet");
    }

    @FXML
    void addNewInvestment(ActionEvent event) {
        System.out.println("Action: Ajouter investissement");
    }
}
