package com.najahni.services;

import com.najahni.dao.CoursDAO;
import com.najahni.models.Cours;
import com.najahni.models.NiveauCours;
import com.najahni.models.TypeCours;

import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Cours.
 * Contient la logique métier pour les opérations sur les cours.
 */
public class CoursService {

    private final CoursDAO coursDAO;

    public CoursService() {
        this.coursDAO = new CoursDAO();
    }

    /**
     * Crée un nouveau cours après validation.
     * @param cours Le cours à créer
     * @return Le cours créé
     * @throws IllegalArgumentException si la validation échoue
     */
    public Cours creerCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        return coursDAO.create(cours);
    }

    /**
     * Met à jour un cours existant.
     * @param cours Le cours à mettre à jour
     * @return true si la mise à jour a réussi
     * @throws IllegalArgumentException si la validation échoue
     */
    public boolean modifierCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        return coursDAO.update(cours);
    }

    /**
     * Supprime un cours.
     * @param id L'ID du cours
     * @return true si la suppression a réussi
     */
    public boolean supprimerCours(int id) {
        return coursDAO.delete(id);
    }

    /**
     * Recherche un cours par son ID.
     * @param id L'ID du cours
     * @return Optional contenant le cours si trouvé
     */
    public Optional<Cours> trouverParId(int id) {
        return coursDAO.findById(id);
    }

    /**
     * Récupère tous les cours.
     * @return Liste de tous les cours
     */
    public List<Cours> trouverTous() {
        return coursDAO.findAll();
    }

    /**
     * Recherche les cours par niveau.
     * @param niveau Le niveau de difficulté
     * @return Liste des cours correspondants
     */
    public List<Cours> trouverParNiveau(NiveauCours niveau) {
        return coursDAO.findByNiveau(niveau);
    }

    /**
     * Recherche les cours par type.
     * @param type Le type de cours
     * @return Liste des cours correspondants
     */
    public List<Cours> trouverParType(TypeCours type) {
        return coursDAO.findByType(type);
    }

    /**
     * Recherche les cours certifiants.
     * @return Liste des cours avec certification
     */
    public List<Cours> trouverCoursCertifiants() {
        return coursDAO.findWithCertification();
    }

    /**
     * Recherche les cours créés par un utilisateur.
     * @param createurId L'ID du créateur
     * @return Liste des cours
     */
    public List<Cours> trouverParCreateur(int createurId) {
        return coursDAO.findByCreateur(createurId);
    }

    /**
     * Recherche les cours par mot-clé.
     * @param motCle Le mot-clé à rechercher
     * @return Liste des cours correspondants
     */
    public List<Cours> rechercher(String motCle) {
        if (motCle == null || motCle.trim().isEmpty()) {
            return trouverTous();
        }
        return coursDAO.search(motCle.trim());
    }

    /**
     * Compte le nombre total de cours.
     * @return Le nombre de cours
     */
    public int compterTous() {
        return coursDAO.count();
    }

    /**
     * Compte le nombre de cours par niveau.
     * @param niveau Le niveau de difficulté
     * @return Le nombre de cours
     */
    public int compterParNiveau(NiveauCours niveau) {
        return coursDAO.countByNiveau(niveau);
    }

    /**
     * Compte le nombre de cours certifiants.
     * @return Le nombre de cours certifiants
     */
    public int compterCertifiants() {
        return (int) coursDAO.findAll().stream()
            .filter(Cours::isCertification)
            .count();
    }

    /**
     * Calcule le total des points XP de tous les cours.
     * @return Le total des points XP
     */
    public int calculerTotalXP() {
        return coursDAO.findAll().stream()
            .mapToInt(Cours::getPointsXP)
            .sum();
    }

    /**
     * Valide les données d'un cours.
     * @param cours Le cours à valider
     * @throws IllegalArgumentException si la validation échoue
     */
    private void validerCours(Cours cours) throws IllegalArgumentException {
        if (cours == null) {
            throw new IllegalArgumentException("Le cours ne peut pas être null.");
        }
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du cours est obligatoire.");
        }
        if (cours.getTitre().length() > 255) {
            throw new IllegalArgumentException("Le titre ne doit pas dépasser 255 caractères.");
        }
        if (cours.getDescription() != null && cours.getDescription().length() > 2000) {
            throw new IllegalArgumentException("La description ne doit pas dépasser 2000 caractères.");
        }
        if (cours.getPointsXP() < 0) {
            throw new IllegalArgumentException("Les points XP doivent être positifs.");
        }
        if (cours.getDureeMinutes() < 0) {
            throw new IllegalArgumentException("La durée estimée doit être positive.");
        }
        if (cours.getNiveau() == null) {
            throw new IllegalArgumentException("Le niveau de difficulté est obligatoire.");
        }
        if (cours.getType() == null) {
            throw new IllegalArgumentException("Le type de cours est obligatoire.");
        }
    }
}
