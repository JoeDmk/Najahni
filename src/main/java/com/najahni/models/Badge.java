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
    private String condition;       // Condition textuelle pour obtenir le badge
    private String conditionType;   // Type de condition (XP, COURS, QUIZ, STREAK, etc.)
    private int conditionValeur;    // Valeur numérique de la condition
    private String icone;           // Emoji ou icône du badge
    private String couleur;         // Couleur du badge (HEX)
    private int pointsBonus;        // Points XP bonus attribués avec le badge
    private boolean actif;
    private LocalDateTime createdAt;

    // Types de conditions prédéfinis
    public static final String CONDITION_XP_TOTAL = "XP_TOTAL";
    public static final String CONDITION_COURS_COMPLETE = "COURS_COMPLETE";
    public static final String CONDITION_NIVEAU_ATTEINT = "NIVEAU_ATTEINT";
    public static final String CONDITION_PREMIER_COURS = "PREMIER_COURS";
    public static final String CONDITION_CERTIFICATION = "CERTIFICATION";

    // Constructeur par défaut
    public Badge() {
        this.actif = true;
        this.pointsBonus = 50;
        this.couleur = "#3498db";
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
                 String conditionType, int conditionValeur, String icone, 
                 String couleur, int pointsBonus) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.condition = condition;
        this.conditionType = conditionType;
        this.conditionValeur = conditionValeur;
        this.icone = icone;
        this.couleur = couleur;
        this.pointsBonus = pointsBonus;
        this.actif = true;
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

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public int getConditionValeur() {
        return conditionValeur;
    }

    public void setConditionValeur(int conditionValeur) {
        this.conditionValeur = conditionValeur;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public int getPointsBonus() {
        return pointsBonus;
    }

    public void setPointsBonus(int pointsBonus) {
        this.pointsBonus = pointsBonus;
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
        if (conditionType == null) return false;
        
        return switch (conditionType) {
            case CONDITION_XP_TOTAL -> xpTotal >= conditionValeur;
            case CONDITION_COURS_COMPLETE -> coursCompletes >= conditionValeur;
            case CONDITION_NIVEAU_ATTEINT -> niveau >= conditionValeur;
            case CONDITION_PREMIER_COURS -> coursCompletes >= 1;
            case CONDITION_CERTIFICATION -> coursCompletes >= conditionValeur;
            default -> false;
        };
    }

    /**
     * Retourne l'affichage complet du badge (icône + nom).
     */
    public String getAffichageComplet() {
        return icone + " " + nom;
    }

    /**
     * Retourne le style CSS pour le badge.
     */
    public String getStyle() {
        return "-fx-background-color: " + couleur + "; -fx-text-fill: white; " +
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
