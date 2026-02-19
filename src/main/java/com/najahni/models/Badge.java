package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Model représentant un Badge de gamification dans NAJAHNI.
 * Les badges sont attribués aux utilisateurs selon des conditions spécifiques.
 */
public class Badge {

    private int id;
    private String nom;
    private String description;
    private String icone;           // Emoji ou icône du badge
    private String condition;       // Condition textuelle (condition_obtention)
    private int pointsRequis;       // Points XP requis pour débloquer
    private int coursRequis;        // Nombre de cours à compléter
    private int niveauRequis;       // Niveau minimum requis
    private String categorie;       // Catégorie du badge (Progression, XP, Niveau, etc.)
    private String rarete;          // Rareté: COMMUN, RARE, EPIQUE, LEGENDAIRE
    private boolean actif;
    private LocalDateTime createdAt;

    // Catégories de badges prédéfinies
    public static final String CATEGORIE_PROGRESSION = "Progression";
    public static final String CATEGORIE_XP = "XP";
    public static final String CATEGORIE_NIVEAU = "Niveau";
    public static final String CATEGORIE_CERTIFICATION = "Certification";

    // Constructeur par défaut
    public Badge() {
        this.actif = true;
        this.pointsRequis = 0;
        this.coursRequis = 0;
        this.niveauRequis = 0;
        this.categorie = "Général";
        this.rarete = "COMMUN";
    }

    // Constructeur sans ID
    public Badge(String nom, String description, String condition, String icone) {
        this();
        this.nom = nom;
        this.description = description;
        this.condition = condition;
        this.icone = icone;
    }

    // Constructeur complet
    public Badge(int id, String nom, String description, String condition, 
                 String icone, int pointsRequis, int coursRequis, int niveauRequis,
                 String categorie, String rarete) {
        this();
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.condition = condition;
        this.icone = icone;
        this.pointsRequis = pointsRequis;
        this.coursRequis = coursRequis;
        this.niveauRequis = niveauRequis;
        this.categorie = categorie;
        this.rarete = rarete;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getPointsRequis() {
        return pointsRequis;
    }

    public void setPointsRequis(int pointsRequis) {
        this.pointsRequis = pointsRequis;
    }

    public int getCoursRequis() {
        return coursRequis;
    }

    public void setCoursRequis(int coursRequis) {
        this.coursRequis = coursRequis;
    }

    public int getNiveauRequis() {
        return niveauRequis;
    }

    public void setNiveauRequis(int niveauRequis) {
        this.niveauRequis = niveauRequis;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getRarete() {
        return rarete;
    }

    public void setRarete(String rarete) {
        this.rarete = rarete;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Vérifie si un utilisateur remplit la condition pour obtenir ce badge.
     */
    public boolean verifierCondition(int xpTotal, int coursCompletes, int niveau) {
        // Check points XP condition
        if (pointsRequis > 0 && xpTotal < pointsRequis) {
            return false;
        }
        // Check cours completed condition
        if (coursRequis > 0 && coursCompletes < coursRequis) {
            return false;
        }
        // Check niveau condition
        if (niveauRequis > 0 && niveau < niveauRequis) {
            return false;
        }
        return true;
    }

    /**
     * Retourne l'affichage complet du badge (icône + nom).
     */
    public String getAffichageComplet() {
        return icone + " " + nom;
    }

    /**
     * Retourne le style CSS pour le badge basé sur la rareté.
     */
    public String getStyle() {
        String color = switch (rarete != null ? rarete : "COMMUN") {
            case "RARE" -> "#3498db";
            case "EPIQUE" -> "#9b59b6";
            case "LEGENDAIRE" -> "#f39c12";
            default -> "#2ecc71"; // COMMUN
        };
        return "-fx-background-color: " + color + "; -fx-text-fill: white; " +
               "-fx-padding: 5 10; -fx-background-radius: 15;";
    }

    @Override
    public String toString() {
        return icone + " " + nom;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Badge badge = (Badge) obj;
        return id == badge.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
