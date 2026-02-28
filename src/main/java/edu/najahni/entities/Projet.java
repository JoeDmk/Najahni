package edu.najahni.entities;

import java.time.LocalDate;

public class Projet {

    private int id;
    private int userId; // 🔥 relation avec table user

    private String titre;
    private String description;
    private String secteur;
    private String etape;
    private String statut;
    private LocalDate dateCreation;

    private donneesBusiness donneesBusiness;

    public Projet() {}

    public Projet(int userId, String titre, String description,
                  String secteur, String etape, String statut,
                  LocalDate dateCreation) {
        this.userId = userId;
        this.titre = titre;
        this.description = description;
        this.secteur = secteur;
        this.etape = etape;
        this.statut = statut;
        this.dateCreation = dateCreation;
    }

    // ================= GETTERS / SETTERS =================

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


    public String getSecteur() {
        return secteur;
    }

    public void setSecteur(String secteur) {
        this.secteur = secteur;
    }


    public String getEtape() {
        return etape;
    }

    public void setEtape(String etape) {
        this.etape = etape;
    }


    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }


    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }


    public donneesBusiness getDonneesBusiness() {
        return donneesBusiness;
    }

    public void setDonneesBusiness(donneesBusiness donneesBusiness) {
        this.donneesBusiness = donneesBusiness;
    }
    // ========== NOUVEAUX CHAMPS POUR MÉTIERS AVANCÉS ==========
    private StatutProjet statutProjet = StatutProjet.BROUILLON;
    private double scoreGlobal;
    private String diagnosticIA;
    private LocalDate dateSoumission;
    private LocalDate dateEvaluation;

// ========== GETTERS/SETTERS (à ajouter à la fin du fichier) ==========

    public StatutProjet getStatutProjet() {
        return statutProjet;
    }

    public void setStatutProjet(StatutProjet statutProjet) {
        this.statutProjet = statutProjet;
    }

    public double getScoreGlobal() {
        return scoreGlobal;
    }

    public void setScoreGlobal(double scoreGlobal) {
        this.scoreGlobal = scoreGlobal;
    }

    public String getDiagnosticIA() {
        return diagnosticIA;
    }

    public void setDiagnosticIA(String diagnosticIA) {
        this.diagnosticIA = diagnosticIA;
    }

    public LocalDate getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(LocalDate dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public LocalDate getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDate dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }
}
