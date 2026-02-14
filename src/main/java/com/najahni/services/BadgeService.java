package com.najahni.services;

import com.najahni.dao.BadgeDAO;
import com.najahni.dao.ProgressionDAO;
import com.najahni.models.Badge;
import com.najahni.models.Progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Badges de gamification.
 * Contient la logique métier pour la gestion des badges.
 */
public class BadgeService {

    private final BadgeDAO badgeDAO;
    private final ProgressionDAO progressionDAO;

    public BadgeService() {
        this.badgeDAO = new BadgeDAO();
        this.progressionDAO = new ProgressionDAO();
    }

    /**
     * Crée un nouveau badge après validation.
     * @param badge Le badge à créer
     * @return Le badge créé
     * @throws IllegalArgumentException si la validation échoue
     */
    public Badge creerBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        return badgeDAO.create(badge);
    }

    /**
     * Met à jour un badge existant.
     * @param badge Le badge à mettre à jour
     * @return true si la mise à jour a réussi
     * @throws IllegalArgumentException si la validation échoue
     */
    public boolean modifierBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        return badgeDAO.update(badge);
    }

    /**
     * Supprime un badge.
     * @param id L'ID du badge
     * @return true si la suppression a réussi
     */
    public boolean supprimerBadge(int id) {
        return badgeDAO.delete(id);
    }

    /**
     * Recherche un badge par son ID.
     * @param id L'ID du badge
     * @return Optional contenant le badge si trouvé
     */
    public Optional<Badge> trouverParId(int id) {
        return badgeDAO.findById(id);
    }

    /**
     * Récupère tous les badges.
     * @return Liste de tous les badges
     */
    public List<Badge> trouverTous() {
        return badgeDAO.findAll();
    }

    /**
     * Récupère les badges actifs.
     * @return Liste des badges actifs
     */
    public List<Badge> trouverActifs() {
        return badgeDAO.findActifs();
    }

    /**
     * Récupère les badges par type de condition.
     * @param conditionType Le type de condition
     * @return Liste des badges correspondants
     */
    public List<Badge> trouverParTypeCondition(String conditionType) {
        return badgeDAO.findByConditionType(conditionType);
    }

    /**
     * Compte le nombre de badges.
     * @return Nombre de badges
     */
    public int compterBadges() {
        return badgeDAO.count();
    }

    /**
     * Compte le nombre de badges actifs.
     * @return Nombre de badges actifs
     */
    public int compterBadgesActifs() {
        return badgeDAO.countActifs();
    }

    /**
     * Compte le nombre de badges obtenus par un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Nombre de badges obtenus
     */
    public int compterBadgesUtilisateur(int userId) {
        List<Badge> badgesEligibles = findBadgesEligibles(userId);
        return badgesEligibles.size();
    }

    /**
     * Récupère les badges éligibles pour un utilisateur basé sur ses statistiques.
     * @param userId L'ID de l'utilisateur
     * @return Liste des badges que l'utilisateur peut obtenir
     */
    public List<Badge> findBadgesEligibles(int userId) {
        int totalXP = progressionDAO.getTotalXPByUser(userId);
        int coursCompletes = progressionDAO.countCoursCompletesByUser(userId);
        
        int niveau = 1;
        for (int i = Progression.SEUILS_NIVEAU.length - 1; i >= 0; i--) {
            if (totalXP >= Progression.SEUILS_NIVEAU[i]) {
                niveau = i + 1;
                break;
            }
        }
        
        List<Badge> allBadges = badgeDAO.findActifs();
        List<Badge> eligibles = new ArrayList<>();
        
        for (Badge badge : allBadges) {
            if (badge.verifierCondition(totalXP, coursCompletes, niveau)) {
                eligibles.add(badge);
            }
        }
        
        return eligibles;
    }

    /**
     * Vérifie et attribue les badges à un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Liste des nouveaux badges obtenus
     */
    public List<Badge> verifierEtAttribuerBadges(int userId) {
        // Récupérer les badges éligibles
        List<Badge> badgesEligibles = findBadgesEligibles(userId);
        
        // Pour le moment, retourne simplement les badges éligibles
        // Dans une implémentation complète, on stockerait l'attribution en base
        return badgesEligibles;
    }

    /**
     * Valide un badge.
     * @param badge Le badge à valider
     * @throws IllegalArgumentException si la validation échoue
     */
    private void validerBadge(Badge badge) throws IllegalArgumentException {
        if (badge == null) {
            throw new IllegalArgumentException("Le badge ne peut pas être null.");
        }
        if (badge.getNom() == null || badge.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du badge est obligatoire.");
        }
        if (badge.getNom().length() > 100) {
            throw new IllegalArgumentException("Le nom du badge ne doit pas dépasser 100 caractères.");
        }
        if (badge.getDescription() != null && badge.getDescription().length() > 500) {
            throw new IllegalArgumentException("La description ne doit pas dépasser 500 caractères.");
        }
        if (badge.getCondition() == null || badge.getCondition().trim().isEmpty()) {
            throw new IllegalArgumentException("La condition d'obtention est obligatoire.");
        }
    }
}
