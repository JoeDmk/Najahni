package edu.najahni.entities;


public class donneesBusiness {
    private int id;
    private String tailleMarche;     // "PETIT", "MOYEN", "GRAND"
    private String modeleRevenu;
    private double coutsEstimes;
    private double revenusAttendus;
    private String niveauRisque;     // "FAIBLE", "MOYEN", "ELEVE"
    private int forceEquipe;         // 0-10
    private int projetId;            // FK

    // Constructeurs vides + pleins

    // Getters / Setters (tous !)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTailleMarche() {
        return tailleMarche;
    }

    public void setTailleMarche(String tailleMarche) {
        this.tailleMarche = tailleMarche;
    }

    public String getModeleRevenu() {
        return modeleRevenu;
    }

    public void setModeleRevenu(String modeleRevenu) {
        this.modeleRevenu = modeleRevenu;
    }

    public double getCoutsEstimes() {
        return coutsEstimes;
    }

    public void setCoutsEstimes(double coutsEstimes) {
        this.coutsEstimes = coutsEstimes;
    }

    public double getRevenusAttendus() {
        return revenusAttendus;
    }

    public void setRevenusAttendus(double revenusAttendus) {
        this.revenusAttendus = revenusAttendus;
    }

    public String getNiveauRisque() {
        return niveauRisque;
    }

    public void setNiveauRisque(String niveauRisque) {
        this.niveauRisque = niveauRisque;
    }

    public int getForceEquipe() {
        return forceEquipe;
    }

    public void setForceEquipe(int forceEquipe) {
        this.forceEquipe = forceEquipe;
    }

    public int getProjetId() {
        return projetId;
    }

    public void setProjetId(int projetId) {
        this.projetId = projetId;
    }
}
