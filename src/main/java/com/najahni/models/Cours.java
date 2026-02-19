package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Model représentant un Cours dans le module d'apprentissage NAJAHNI.
 * Un cours peut contenir du contenu vidéo, texte ou quiz.
 */
public class Cours {

    private int id;
    private String titre;
    private String description;
    private TypeCours type;
    private NiveauCours niveau;
    private boolean certification;
    private int pointsXP;           // Points XP gagnés en complétant le cours
    private int dureeMinutes;       // Durée estimée du cours en minutes
    private String imageUrl;        // URL de l'image de couverture
    private int createurId;         // ID de l'utilisateur qui a créé le cours
    private String createurNom;     // Nom du créateur (pour affichage)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructeur par défaut
    public Cours() {
        this.type = TypeCours.TEXTE;
        this.niveau = NiveauCours.DEBUTANT;
        this.certification = false;
        this.pointsXP = 100;
        this.dureeMinutes = 30;
    }

    // Constructeur sans ID (pour création)
    public Cours(String titre, String description, TypeCours type, NiveauCours niveau, boolean certification) {
        this();
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.niveau = niveau;
        this.certification = certification;
        this.pointsXP = calculerPointsXPParDefaut(niveau);
    }

    // Constructeur complet
    public Cours(int id, String titre, String description, TypeCours type, NiveauCours niveau, 
                 boolean certification, int pointsXP, int dureeMinutes, int createurId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.niveau = niveau;
        this.certification = certification;
        this.pointsXP = pointsXP;
        this.dureeMinutes = dureeMinutes;
        this.createurId = createurId;
    }

    /**
     * Calcule les points XP par défaut selon le niveau du cours.
     */
    private int calculerPointsXPParDefaut(NiveauCours niveau) {
        return switch (niveau) {
            case DEBUTANT -> 100;
            case INTERMEDIAIRE -> 200;
            case AVANCE -> 350;
            case EXPERT -> 500;
        };
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypeCours getType() {
        return type;
    }

    public void setType(TypeCours type) {
        this.type = type;
    }

    public NiveauCours getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauCours niveau) {
        this.niveau = niveau;
    }

    public boolean isCertification() {
        return certification;
    }

    public void setCertification(boolean certification) {
        this.certification = certification;
    }

    public int getPointsXP() {
        return pointsXP;
    }

    public void setPointsXP(int pointsXP) {
        this.pointsXP = pointsXP;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(int dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getCreateurId() {
        return createurId;
    }

    public void setCreateurId(int createurId) {
        this.createurId = createurId;
    }

    public String getCreateurNom() {
        return createurNom;
    }

    public void setCreateurNom(String createurNom) {
        this.createurNom = createurNom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Retourne la durée formatée du cours.
     */
    public String getDureeFormatee() {
        if (dureeMinutes < 60) {
            return dureeMinutes + " min";
        } else {
            int heures = dureeMinutes / 60;
            int minutes = dureeMinutes % 60;
            return heures + "h" + (minutes > 0 ? minutes : "");
        }
    }

    /**
     * Retourne l'icône correspondant au type de cours.
     */
    public String getTypeIcon() {
        return switch (type) {
            case VIDEO -> "🎬";
            case TEXTE -> "📖";
            case QUIZ -> "❓";
            case MIXTE -> "📚";
        };
    }

    /**
     * Retourne l'icône du niveau.
     */
    public String getNiveauIcon() {
        return switch (niveau) {
            case DEBUTANT -> "🌱";
            case INTERMEDIAIRE -> "🌿";
            case AVANCE -> "🌳";
            case EXPERT -> "🏆";
        };
    }

    @Override
    public String toString() {
        return titre + " (" + niveau.getDisplayName() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Cours cours = (Cours) obj;
        return id == cours.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
