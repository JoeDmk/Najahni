package edu.najahni.entities;



import java.time.LocalDate;

public class Projet {
    private int id;
    private String titre;
    private String description;
    private String secteur;      // ex. "FinTech", "GreenTech"
    private String etape;        // "BROUILLON", "SOUMIS", "EVALUE"
    private String statut;
    private LocalDate dateCreation;
    private donneesBusiness donneesBusiness;  // relation 1-1

    // Constructeurs
    public Projet() {}
    public Projet(String titre, String description, String secteur, String etape) {
        this.titre = titre;
        this.description = description;
        this.secteur = secteur;
        this.etape = etape;
        this.dateCreation = LocalDate.now();
    }

    // Getters / Setters obligatoires (pour PropertyValueFactory dans TableView)

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

    // ... idem pour tous les autres champs

    public donneesBusiness getDonneesBusiness() { return donneesBusiness; }
    public void setDonneesBusiness(donneesBusiness db) { this.donneesBusiness = db; }
}