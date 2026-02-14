package com.najahni.models;

import java.time.LocalDateTime;

public class SessionMentorat {
    private int id;
    private LocalDateTime dateDemande;
    private String statut; // En attente, Acceptée, Refusée
    private double scoreMatchingAI;

    private int entrepreneurId;
    private Integer mentorId; // Peut être null avant matching IA
    private int projetId;


    public SessionMentorat() {
    }

    public SessionMentorat(int id, LocalDateTime dateDemande, String statut, double scoreMatchingAI, int entrepreneurId, Integer mentorId, int projetId) {
        this.id = id;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.scoreMatchingAI = scoreMatchingAI;
        this.entrepreneurId = entrepreneurId;
        this.mentorId = mentorId;
        this.projetId = projetId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getScoreMatchingAI() { return scoreMatchingAI; }
    public void setScoreMatchingAI(double scoreMatchingAI) { this.scoreMatchingAI = scoreMatchingAI; }

    public int getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(int entrepreneurId) { this.entrepreneurId = entrepreneurId; }

    public Integer getMentorId() { return mentorId; }
    public void setMentorId(Integer mentorId) { this.mentorId = mentorId; }

    public int getProjetId() { return projetId; }
    public void setProjetId(int projetId) { this.projetId = projetId; }
}
