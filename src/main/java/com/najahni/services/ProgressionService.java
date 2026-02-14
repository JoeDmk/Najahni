package com.najahni.services;

import com.najahni.dao.ProgressionDAO;
import com.najahni.dao.CoursDAO;
import com.najahni.models.Cours;
import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Progressions.
 * Contient la logique métier pour le suivi de progression des utilisateurs.
 */
public class ProgressionService {

    private final ProgressionDAO progressionDAO;
    private final CoursDAO coursDAO;
    private final BadgeService badgeService;

    public ProgressionService() {
        this.progressionDAO = new ProgressionDAO();
        this.coursDAO = new CoursDAO();
        this.badgeService = new BadgeService();
    }

    /**
     * Démarre ou récupère la progression d'un utilisateur pour un cours.
     * @param userId L'ID de l'utilisateur
     * @param coursId L'ID du cours
     * @return La progression
     */
    public Progression demarrerCours(int userId, int coursId) {
        // Vérifier si une progression existe déjà
        Optional<Progression> existante = progressionDAO.findByUserAndCours(userId, coursId);
        if (existante.isPresent()) {
            return existante.get();
        }

        // Créer une nouvelle progression
        Progression progression = new Progression(userId, coursId);
        progression.setEtat(EtatProgression.EN_COURS);
        
        return progressionDAO.create(progression);
    }

    /**
     * Met à jour manuellement le pourcentage de progression.
     * @param progressionId L'ID de la progression
     * @param nouveauPourcentage Le nouveau pourcentage
     * @return true si la mise à jour a réussi
     */
    public boolean mettreAJourPourcentage(int progressionId, double nouveauPourcentage) {
        Optional<Progression> progressionOpt = progressionDAO.findById(progressionId);
        if (progressionOpt.isEmpty()) {
            return false;
        }
        
        Progression progression = progressionOpt.get();
        progression.setPourcentage(nouveauPourcentage);
        
        if (nouveauPourcentage >= 100.0 && progression.getDateObtention() == null) {
            progression.setDateObtention(LocalDateTime.now());
            progression.setEtat(EtatProgression.COMPLETE);
        }
        
        return progressionDAO.update(progression);
    }

    /**
     * Met à jour la progression avec points XP.
     * @param progressionId L'ID de la progression
     * @param nouveauPourcentage Le nouveau pourcentage
     * @param pointsXP Les points XP à ajouter
     * @return true si la mise à jour a réussi
     */
    public boolean mettreAJourProgression(int progressionId, double nouveauPourcentage, int pointsXP) {
        Optional<Progression> progressionOpt = progressionDAO.findById(progressionId);
        if (progressionOpt.isEmpty()) {
            return false;
        }
        
        Progression progression = progressionOpt.get();
        progression.setPourcentage(nouveauPourcentage);
        progression.ajouterPointsXP(pointsXP);
        
        if (nouveauPourcentage >= 100.0 && progression.getDateObtention() == null) {
            progression.setDateObtention(LocalDateTime.now());
            progression.setEtat(EtatProgression.COMPLETE);
            
            // Vérifier les badges
            badgeService.verifierEtAttribuerBadges(progression.getUserId());
        }
        
        return progressionDAO.update(progression);
    }

    /**
     * Complète un cours et attribue la certification si applicable.
     * @param userId L'ID de l'utilisateur
     * @param coursId L'ID du cours
     * @return La progression mise à jour
     */
    public Progression completerCours(int userId, int coursId) {
        Progression progression = demarrerCours(userId, coursId);
        progression.setPourcentage(100.0);
        progression.setEtat(EtatProgression.COMPLETE);
        progression.setDateObtention(LocalDateTime.now());
        
        // Ajouter les points XP du cours
        Optional<Cours> coursOpt = coursDAO.findById(coursId);
        if (coursOpt.isPresent()) {
            Cours cours = coursOpt.get();
            progression.ajouterPointsXP(cours.getPointsXP());
            
            // Si le cours est certifiant, marquer comme certifié
            if (cours.isCertification()) {
                progression.setEtat(EtatProgression.CERTIFIE);
            }
        }
        
        progressionDAO.update(progression);
        
        // Vérifier les badges
        badgeService.verifierEtAttribuerBadges(userId);
        
        return progression;
    }

    /**
     * Récupère toutes les progressions d'un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Liste des progressions
     */
    public List<Progression> trouverParUtilisateur(int userId) {
        return progressionDAO.findByUserId(userId);
    }

    /**
     * Récupère la progression d'un utilisateur pour un cours.
     * @param userId L'ID de l'utilisateur
     * @param coursId L'ID du cours
     * @return Optional contenant la progression si trouvée
     */
    public Optional<Progression> trouverParUtilisateurEtCours(int userId, int coursId) {
        return progressionDAO.findByUserAndCours(userId, coursId);
    }

    /**
     * Récupère les progressions par état.
     * @param etat L'état recherché
     * @return Liste des progressions
     */
    public List<Progression> trouverParEtat(EtatProgression etat) {
        return progressionDAO.findByEtat(etat);
    }

    /**
     * Calcule le total des points XP d'un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Total des points XP
     */
    public int getTotalXP(int userId) {
        return progressionDAO.getTotalXPByUser(userId);
    }

    /**
     * Calcule le niveau global d'un utilisateur basé sur son XP total.
     * @param userId L'ID de l'utilisateur
     * @return Le niveau de l'utilisateur
     */
    public int getNiveauGlobal(int userId) {
        int totalXP = getTotalXP(userId);
        for (int i = Progression.SEUILS_NIVEAU.length - 1; i >= 0; i--) {
            if (totalXP >= Progression.SEUILS_NIVEAU[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Compte le nombre de cours complétés par un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Nombre de cours complétés
     */
    public int getNombreCoursCompletes(int userId) {
        return progressionDAO.countCoursCompletesByUser(userId);
    }

    /**
     * Récupère les statistiques d'un utilisateur.
     * @param userId L'ID de l'utilisateur
     * @return Tableau [totalXP, niveau, coursCompletes, coursEnCours]
     */
    public int[] getStatistiquesUtilisateur(int userId) {
        int[] stats = new int[4];
        stats[0] = getTotalXP(userId);
        stats[1] = getNiveauGlobal(userId);
        stats[2] = progressionDAO.countCoursCompletesByUser(userId);
        stats[3] = progressionDAO.findByUserId(userId).stream()
            .filter(p -> p.getEtat() == EtatProgression.EN_COURS)
            .mapToInt(p -> 1).sum();
        return stats;
    }

    /**
     * Récupère le classement des utilisateurs (leaderboard).
     * @param limit Nombre maximum d'entrées
     * @return Liste des entrées du classement
     */
    public List<Object[]> getLeaderboard(int limit) {
        return progressionDAO.getLeaderboard(limit);
    }

    /**
     * Récupère le classement par défaut (top 10).
     * @return Liste des entrées du classement
     */
    public List<Object[]> getLeaderboard() {
        return getLeaderboard(10);
    }

    /**
     * Recherche toutes les progressions.
     * @return Liste de toutes les progressions
     */
    public List<Progression> trouverToutes() {
        return progressionDAO.findAll();
    }

    /**
     * Recherche une progression par ID.
     * @param id L'ID de la progression
     * @return Optional contenant la progression si trouvée
     */
    public Optional<Progression> trouverParId(int id) {
        return progressionDAO.findById(id);
    }

    /**
     * Crée une nouvelle progression.
     * @param progression La progression à créer
     * @return La progression créée
     */
    public Progression creer(Progression progression) {
        return progressionDAO.create(progression);
    }

    /**
     * Met à jour une progression.
     * @param progression La progression à mettre à jour
     * @return true si la mise à jour a réussi
     */
    public boolean mettreAJour(Progression progression) {
        return progressionDAO.update(progression);
    }

    /**
     * Supprime une progression.
     * @param id L'ID de la progression
     * @return true si la suppression a réussi
     */
    public boolean supprimer(int id) {
        return progressionDAO.delete(id);
    }
}
