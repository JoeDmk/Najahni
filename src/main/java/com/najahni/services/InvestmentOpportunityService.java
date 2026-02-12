package com.najahni.services;

import com.najahni.dao.InvestmentOpportunityDAO;
import com.najahni.dao.ProjectDAO;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour les Opportunités d'Investissement.
 * 
 * Contient TOUTE la logique métier et les validations.
 * Aucune logique UI (pas d'Alert, pas de FXML).
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class InvestmentOpportunityService {

    private final InvestmentOpportunityDAO opportunityDAO;
    private final ProjectDAO projectDAO;

    /** Constructeur par défaut (instancie les DAO). */
    public InvestmentOpportunityService() {
        this.opportunityDAO = new InvestmentOpportunityDAO();
        this.projectDAO = new ProjectDAO();
    }

    /**
     * Constructeur avec injection de dépendances (pour les tests unitaires).
     */
    public InvestmentOpportunityService(InvestmentOpportunityDAO opportunityDAO, ProjectDAO projectDAO) {
        this.opportunityDAO = opportunityDAO;
        this.projectDAO = projectDAO;
    }

    // ─── CRUD ────────────────────────────────────────────────

    /**
     * Crée une nouvelle opportunité après validation complète.
     * @throws IllegalArgumentException si la validation échoue
     */
    public InvestmentOpportunity createOpportunity(InvestmentOpportunity opp) {
        validateOpportunity(opp);
        validateProjectExists(opp.getProjectId());
        return opportunityDAO.create(opp);
    }

    /**
     * Met à jour une opportunité existante après validation.
     * @throws IllegalArgumentException si la validation échoue
     */
    public boolean updateOpportunity(InvestmentOpportunity opp) {
        validateOpportunity(opp);
        validateProjectExists(opp.getProjectId());
        return opportunityDAO.update(opp);
    }

    /**
     * Supprime une opportunité par son ID.
     * La suppression en cascade supprimera aussi les offres liées.
     */
    public boolean deleteOpportunity(int id) {
        return opportunityDAO.delete(id);
    }

    /** Trouve une opportunité par ID. */
    public Optional<InvestmentOpportunity> findById(int id) {
        return opportunityDAO.findById(id);
    }

    /** Retourne toutes les opportunités. */
    public List<InvestmentOpportunity> findAll() {
        return opportunityDAO.findAll();
    }

    /** Retourne les opportunités liées à un projet. */
    public List<InvestmentOpportunity> findByProject(int projectId) {
        return opportunityDAO.findByProjectId(projectId);
    }

    /** Retourne les opportunités avec un statut donné. */
    public List<InvestmentOpportunity> findByStatus(OpportunityStatus status) {
        return opportunityDAO.findByStatus(status);
    }

    // ─── STATISTIQUES ────────────────────────────────────────

    /** Compte les opportunités par statut. */
    public int countByStatus(OpportunityStatus status) {
        return opportunityDAO.countByStatus(status);
    }

    /** Retourne le montant total cible des opportunités ouvertes. */
    public BigDecimal getTotalTargetAmount() {
        return opportunityDAO.getTotalTargetAmount();
    }

    // ─── OPÉRATIONS MÉTIER ───────────────────────────────────

    /**
     * Ferme une opportunité (passe son statut à CLOSED).
     */
    public boolean closeOpportunity(int opportunityId) {
        return updateStatus(opportunityId, OpportunityStatus.CLOSED);
    }

    /**
     * Marque une opportunité comme financée (FUNDED).
     */
    public boolean markAsFunded(int opportunityId) {
        return updateStatus(opportunityId, OpportunityStatus.FUNDED);
    }

    /**
     * Met à jour le statut d'une opportunité.
     */
    private boolean updateStatus(int opportunityId, OpportunityStatus newStatus) {
        Optional<InvestmentOpportunity> oppOpt = opportunityDAO.findById(opportunityId);
        if (oppOpt.isPresent()) {
            InvestmentOpportunity opp = oppOpt.get();
            opp.setStatus(newStatus);
            return opportunityDAO.update(opp);
        }
        return false;
    }

    // ─── VALIDATIONS ─────────────────────────────────────────

    /**
     * Valide les données d'une opportunité.
     * @throws IllegalArgumentException si une règle est violée
     */
    private void validateOpportunity(InvestmentOpportunity opp) {
        if (opp == null) {
            throw new IllegalArgumentException("L'opportunité ne peut pas être null.");
        }

        // Validation du montant cible
        if (opp.getTargetAmount() == null) {
            throw new IllegalArgumentException("Le montant cible est obligatoire.");
        }
        if (opp.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être supérieur à zéro.");
        }
        if (opp.getTargetAmount().compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("Le montant cible dépasse la valeur maximale autorisée.");
        }

        // Validation du statut
        if (opp.getStatus() == null) {
            throw new IllegalArgumentException("Le statut est obligatoire.");
        }

        // Validation du projet
        if (opp.getProjectId() <= 0) {
            throw new IllegalArgumentException("Un projet valide est requis.");
        }

        // Validation de la deadline (si fournie)
        if (opp.getDeadline() != null && opp.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La deadline ne peut pas être dans le passé.");
        }
    }

    /**
     * Vérifie que le projet existe.
     * @throws IllegalArgumentException si le projet n'existe pas
     */
    private void validateProjectExists(int projectId) {
        Optional<Project> projectOpt = projectDAO.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Le projet sélectionné n'existe pas.");
        }
    }
}
