package edu.najahni.entities;

public class donneesBusiness {
    private int id;
    private String tailleMarche;
    private String modeleRevenu;
    private double coutsEstimes;
    private double revenusAttendus;
    private String niveauRisque;
    private int forceEquipe;
    private int projetId;

    // ========== CHAMPS RAW — TEXTE EXACT DE L'UTILISATEUR ==========
    // Ces champs stockent ce que l'utilisateur a VRAIMENT tapé,
    // avant toute tentative de parsing numérique.
    // Si l'user tape "je sais pas" → rawCouts = "je sais pas", coutsEstimes = 0
    // Si l'user tape "50000"       → rawCouts = "50000",        coutsEstimes = 50000.0
    // L'IA lit rawCouts et comprend l'intention réelle.
    private String rawCouts;
    private String rawRevenus;
    private String rawForceEquipe;

    // ========== INDICATEURS CALCULÉS ==========
    private double margeEstimee;
    private double ratioRentabilite;
    private double scoreFinancier;
    private double scoreMarche;
    private double scoreEquipeCalcule;
    private double scoreRisqueCalcule;

    // ========== GETTERS / SETTERS — CHAMPS DE BASE ==========

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTailleMarche() { return tailleMarche; }
    public void setTailleMarche(String tailleMarche) { this.tailleMarche = tailleMarche; }

    public String getModeleRevenu() { return modeleRevenu; }
    public void setModeleRevenu(String modeleRevenu) { this.modeleRevenu = modeleRevenu; }

    public double getCoutsEstimes() { return coutsEstimes; }
    public void setCoutsEstimes(double coutsEstimes) { this.coutsEstimes = coutsEstimes; }

    public double getRevenusAttendus() { return revenusAttendus; }
    public void setRevenusAttendus(double revenusAttendus) { this.revenusAttendus = revenusAttendus; }

    public String getNiveauRisque() { return niveauRisque; }
    public void setNiveauRisque(String niveauRisque) { this.niveauRisque = niveauRisque; }

    public int getForceEquipe() { return forceEquipe; }
    public void setForceEquipe(int forceEquipe) { this.forceEquipe = forceEquipe; }

    public int getProjetId() { return projetId; }
    public void setProjetId(int projetId) { this.projetId = projetId; }

    // ========== GETTERS / SETTERS — CHAMPS RAW ==========

    public String getRawCouts() { return rawCouts; }
    public void setRawCouts(String rawCouts) { this.rawCouts = rawCouts; }

    public String getRawRevenus() { return rawRevenus; }
    public void setRawRevenus(String rawRevenus) { this.rawRevenus = rawRevenus; }

    public String getRawForceEquipe() { return rawForceEquipe; }
    public void setRawForceEquipe(String rawForceEquipe) { this.rawForceEquipe = rawForceEquipe; }

    // ========== GETTERS / SETTERS — INDICATEURS ==========

    public double getMargeEstimee() { return margeEstimee; }
    public void setMargeEstimee(double margeEstimee) { this.margeEstimee = margeEstimee; }

    public double getRatioRentabilite() { return ratioRentabilite; }
    public void setRatioRentabilite(double ratioRentabilite) { this.ratioRentabilite = ratioRentabilite; }

    public double getScoreFinancier() { return scoreFinancier; }
    public void setScoreFinancier(double scoreFinancier) { this.scoreFinancier = scoreFinancier; }

    public double getScoreMarche() { return scoreMarche; }
    public void setScoreMarche(double scoreMarche) { this.scoreMarche = scoreMarche; }

    public double getScoreEquipeCalcule() { return scoreEquipeCalcule; }
    public void setScoreEquipeCalcule(double scoreEquipeCalcule) { this.scoreEquipeCalcule = scoreEquipeCalcule; }

    public double getScoreRisqueCalcule() { return scoreRisqueCalcule; }
    public void setScoreRisqueCalcule(double scoreRisqueCalcule) { this.scoreRisqueCalcule = scoreRisqueCalcule; }

    // ========== CALCUL DES INDICATEURS ==========
    public void calculerIndicateurs() {
        this.margeEstimee    = revenusAttendus - coutsEstimes;
        this.ratioRentabilite = coutsEstimes > 0 ? revenusAttendus / coutsEstimes : 0;

        // Score financier (sur 40)
        this.scoreFinancier = 0;
        if (ratioRentabilite >= 3)   scoreFinancier += 20;
        else if (ratioRentabilite >= 2)   scoreFinancier += 15;
        else if (ratioRentabilite >= 1.5) scoreFinancier += 10;
        else if (ratioRentabilite >= 1)   scoreFinancier += 5;

        if (margeEstimee > 100000) scoreFinancier += 20;
        else if (margeEstimee > 50000)  scoreFinancier += 15;
        else if (margeEstimee > 10000)  scoreFinancier += 10;
        else if (margeEstimee > 0)      scoreFinancier += 5;

        // Score marché (sur 30)
        this.scoreMarche = 0;
        if (tailleMarche != null) {
            String t = tailleMarche.toLowerCase();
            if (t.contains("grand") || t.contains("large") || t.contains("national")) scoreMarche = 30;
            else if (t.contains("moyen") || t.contains("medium") || t.contains("régional"))  scoreMarche = 20;
            else if (t.contains("petit") || t.contains("niche"))  scoreMarche = 10;
            else scoreMarche = 12; // valeur textuelle non standard mais renseignée
        }

        // Score équipe (sur 30)
        this.scoreEquipeCalcule = forceEquipe * 3;

        // Score risque (sur 30)
        this.scoreRisqueCalcule = 0;
        if (niveauRisque != null) {
            String r = niveauRisque.toLowerCase();
            if (r.contains("faible") || r.contains("low"))        scoreRisqueCalcule = 30;
            else if (r.contains("moyen") || r.contains("medium")) scoreRisqueCalcule = 20;
            else if (r.contains("elev") || r.contains("high"))    scoreRisqueCalcule = 10;
            else scoreRisqueCalcule = 15; // valeur textuelle non standard
        }

        System.out.println("✅ Indicateurs calculés — Fin: " + scoreFinancier
                + ", Marché: " + scoreMarche + ", Équipe: " + scoreEquipeCalcule
                + ", Risque: " + scoreRisqueCalcule);
    }
}