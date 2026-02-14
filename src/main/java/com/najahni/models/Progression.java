package com.najahni.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model représentant la progression d'un utilisateur dans un cours.
 * Suit les points XP, le niveau et le pourcentage de complétion.
 */
public class Progression {

    private int id;
    private int userId;
    private String userNom;         // Pour affichage
    private int coursId;
    private String coursTitre;      // Pour affichage
    private double pourcentage;     // 0.0 à 100.0
    private int pointsXP;
    private int niveau;
    private EtatProgression etat;
    private LocalDateTime dateDebut;
    private LocalDateTime dateObtention;    // Date de complétion
    private LocalDateTime updatedAt;

    // Seuils de niveau (XP cumulés nécessaires)
    public static final int[] SEUILS_NIVEAU = {0, 100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500};

    // Constructeur par défaut
    public Progression() {
        this.pourcentage = 0.0;
        this.pointsXP = 0;
        this.niveau = 1;
        this.etat = EtatProgression.NON_COMMENCE;
        this.dateDebut = LocalDateTime.now();
    }

    // Constructeur sans ID
    public Progression(int userId, int coursId) {
        this();
        this.userId = userId;
        this.coursId = coursId;
    }

    // Constructeur complet
    public Progression(int id, int userId, int coursId, double pourcentage, 
                       int pointsXP, int niveau, EtatProgression etat) {
        this.id = id;
        this.userId = userId;
        this.coursId = coursId;
        this.pourcentage = pourcentage;
        this.pointsXP = pointsXP;
        this.niveau = niveau;
        this.etat = etat;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserNom() {
        return userNom;
    }

    public void setUserNom(String userNom) {
        this.userNom = userNom;
    }

    public int getCoursId() {
        return coursId;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public String getCoursTitre() {
        return coursTitre;
    }

    public void setCoursTitre(String coursTitre) {
        this.coursTitre = coursTitre;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    public void setPourcentage(double pourcentage) {
        this.pourcentage = Math.max(0.0, Math.min(100.0, pourcentage));
        updateEtat();
    }

    public int getPointsXP() {
        return pointsXP;
    }

    public void setPointsXP(int pointsXP) {
        this.pointsXP = pointsXP;
        recalculerNiveau();
    }

    public int getNiveau() {
        return niveau;
    }

    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }

    public EtatProgression getEtat() {
        return etat;
    }

    public void setEtat(EtatProgression etat) {
        this.etat = etat;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateObtention() {
        return dateObtention;
    }

    public void setDateObtention(LocalDateTime dateObtention) {
        this.dateObtention = dateObtention;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Ajoute des points XP et recalcule le niveau.
     */
    public void ajouterPointsXP(int points) {
        this.pointsXP += points;
        recalculerNiveau();
    }

    /**
     * Recalcule le niveau en fonction des points XP.
     */
    public void recalculerNiveau() {
        for (int i = SEUILS_NIVEAU.length - 1; i >= 0; i--) {
            if (pointsXP >= SEUILS_NIVEAU[i]) {
                this.niveau = i + 1;
                break;
            }
        }
    }

    /**
     * Met à jour l'état en fonction du pourcentage.
     */
    private void updateEtat() {
        if (pourcentage == 0.0) {
            this.etat = EtatProgression.NON_COMMENCE;
        } else if (pourcentage < 100.0) {
            this.etat = EtatProgression.EN_COURS;
        } else {
            this.etat = EtatProgression.COMPLETE;
            if (dateObtention == null) {
                dateObtention = LocalDateTime.now();
            }
        }
    }

    /**
     * Retourne les points XP nécessaires pour le prochain niveau.
     */
    public int getPointsProchainNiveau() {
        if (niveau >= SEUILS_NIVEAU.length) {
            return 0; // Niveau max atteint
        }
        return SEUILS_NIVEAU[niveau] - pointsXP;
    }

    /**
     * Retourne le pourcentage de progression vers le prochain niveau.
     */
    public double getProgressionNiveau() {
        if (niveau >= SEUILS_NIVEAU.length) {
            return 100.0;
        }
        int seuilActuel = SEUILS_NIVEAU[niveau - 1];
        int seuilProchain = SEUILS_NIVEAU[niveau];
        int pointsDansNiveau = pointsXP - seuilActuel;
        int pointsNecessaires = seuilProchain - seuilActuel;
        return (pointsDansNiveau * 100.0) / pointsNecessaires;
    }

    /**
     * Retourne le pourcentage formaté.
     */
    public String getPourcentageFormate() {
        return String.format("%.1f%%", pourcentage);
    }

    /**
     * Retourne la date d'obtention formatée.
     */
    public String getDateObtentionFormatee() {
        if (dateObtention == null) {
            return "En cours";
        }
        return dateObtention.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Vérifie si le cours est complété.
     */
    public boolean estComplete() {
        return pourcentage >= 100.0 || etat == EtatProgression.COMPLETE;
    }

    @Override
    public String toString() {
        return "Progression [" + coursTitre + "] - " + getPourcentageFormate() + " (Niveau " + niveau + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Progression that = (Progression) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
