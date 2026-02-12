package com.najahni.services;

import com.najahni.dao.InvestmentOfferDAO;
import com.najahni.dao.InvestmentOpportunityDAO;
import com.najahni.dao.UserDAO;
import com.najahni.models.InvestmentOffer;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OfferStatus;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Role;
import com.najahni.models.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour les Offres d'Investissement.
 * 
 * Contient TOUTE la logique métier et les validations.
 * Aucune logique UI (pas d'Alert, pas de FXML).
 * 
 * Règles métier :
 * - Seul un INVESTOR peut créer une offre.
 * - Une offre ne peut être faite que sur une opportunité OPEN.
 * - Le montant proposé doit être > 0.
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class InvestmentOfferService {

    private final InvestmentOfferDAO offerDAO;
    private final InvestmentOpportunityDAO opportunityDAO;
    private final UserDAO userDAO;

    /** Constructeur par défaut (instancie les DAO). */
    public InvestmentOfferService() {
        this.offerDAO = new InvestmentOfferDAO();
        this.opportunityDAO = new InvestmentOpportunityDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Constructeur avec injection de dépendances (pour les tests unitaires).
     */
    public InvestmentOfferService(InvestmentOfferDAO offerDAO,
                                   InvestmentOpportunityDAO opportunityDAO,
                                   UserDAO userDAO) {
        this.offerDAO = offerDAO;
        this.opportunityDAO = opportunityDAO;
        this.userDAO = userDAO;
    }

    // ─── CRUD ────────────────────────────────────────────────

    /**
     * Crée une nouvelle offre après validation complète.
     * @throws IllegalArgumentException si la validation échoue
     */
    public InvestmentOffer createOffer(InvestmentOffer offer) {
        validateOffer(offer);
        validateInvestorRole(offer.getInvestorId());
        validateOpportunityIsOpen(offer.getOpportunityId());
        return offerDAO.create(offer);
    }

    /**
     * Met à jour une offre existante après validation.
     * @throws IllegalArgumentException si la validation échoue
     */
    public boolean updateOffer(InvestmentOffer offer) {
        validateOffer(offer);
        return offerDAO.update(offer);
    }

    /**
     * Supprime une offre par son ID.
     */
    public boolean deleteOffer(int id) {
        return offerDAO.delete(id);
    }

    /** Trouve une offre par ID. */
    public Optional<InvestmentOffer> findById(int id) {
        return offerDAO.findById(id);
    }

    /** Retourne toutes les offres. */
    public List<InvestmentOffer> findAll() {
        return offerDAO.findAll();
    }

    /** Retourne les offres liées à une opportunité. */
    public List<InvestmentOffer> findByOpportunity(int opportunityId) {
        return offerDAO.findByOpportunityId(opportunityId);
    }

    /** Retourne les offres d'un investisseur. */
    public List<InvestmentOffer> findByInvestor(int investorId) {
        return offerDAO.findByInvestorId(investorId);
    }

    // ─── STATISTIQUES ────────────────────────────────────────

    /** Compte les offres par statut. */
    public int countByStatus(OfferStatus status) {
        return offerDAO.countByStatus(status);
    }

    /** Retourne le montant total accepté pour une opportunité. */
    public BigDecimal getTotalAcceptedForOpportunity(int opportunityId) {
        return offerDAO.getTotalAcceptedForOpportunity(opportunityId);
    }

    // ─── OPÉRATIONS MÉTIER ───────────────────────────────────

    /**
     * Accepte une offre (passe son statut à ACCEPTED).
     */
    public boolean acceptOffer(int offerId) {
        return updateStatus(offerId, OfferStatus.ACCEPTED);
    }

    /**
     * Rejette une offre (passe son statut à REJECTED).
     */
    public boolean rejectOffer(int offerId) {
        return updateStatus(offerId, OfferStatus.REJECTED);
    }

    /**
     * Met à jour le statut d'une offre.
     */
    private boolean updateStatus(int offerId, OfferStatus newStatus) {
        Optional<InvestmentOffer> offerOpt = offerDAO.findById(offerId);
        if (offerOpt.isPresent()) {
            InvestmentOffer offer = offerOpt.get();
            offer.setStatus(newStatus);
            return offerDAO.update(offer);
        }
        return false;
    }

    // ─── VALIDATIONS ─────────────────────────────────────────

    /**
     * Valide les données d'une offre.
     * @throws IllegalArgumentException si une règle est violée
     */
    private void validateOffer(InvestmentOffer offer) {
        if (offer == null) {
            throw new IllegalArgumentException("L'offre ne peut pas être null.");
        }

        // Validation du montant proposé
        if (offer.getProposedAmount() == null) {
            throw new IllegalArgumentException("Le montant proposé est obligatoire.");
        }
        if (offer.getProposedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant proposé doit être supérieur à zéro.");
        }
        if (offer.getProposedAmount().compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("Le montant proposé dépasse la valeur maximale autorisée.");
        }

        // Validation du statut
        if (offer.getStatus() == null) {
            throw new IllegalArgumentException("Le statut est obligatoire.");
        }

        // Validation de l'investisseur
        if (offer.getInvestorId() <= 0) {
            throw new IllegalArgumentException("Un investisseur valide est requis.");
        }

        // Validation de l'opportunité
        if (offer.getOpportunityId() <= 0) {
            throw new IllegalArgumentException("Une opportunité valide est requise.");
        }
    }

    /**
     * Vérifie que l'utilisateur a le rôle INVESTOR.
     * @throws IllegalArgumentException si l'utilisateur n'est pas un INVESTOR
     */
    private void validateInvestorRole(int userId) {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("L'utilisateur n'existe pas.");
        }
        User user = userOpt.get();
        if (user.getRole() != Role.INVESTOR) {
            throw new IllegalArgumentException("Seul un INVESTOR peut créer une offre d'investissement.");
        }
    }

    /**
     * Vérifie que l'opportunité est ouverte (statut OPEN).
     * @throws IllegalArgumentException si l'opportunité n'est pas OPEN
     */
    private void validateOpportunityIsOpen(int opportunityId) {
        Optional<InvestmentOpportunity> oppOpt = opportunityDAO.findById(opportunityId);
        if (oppOpt.isEmpty()) {
            throw new IllegalArgumentException("L'opportunité d'investissement n'existe pas.");
        }
        InvestmentOpportunity opp = oppOpt.get();
        if (opp.getStatus() != OpportunityStatus.OPEN) {
            throw new IllegalArgumentException("Impossible de faire une offre : l'opportunité n'est pas ouverte (statut actuel : " + opp.getStatus() + ").");
        }
    }
}
