package models.apprentissage;

import java.time.LocalDateTime;

/**
 * Model représentant un Badge de gamification dans NAJAHNI.
 * Les badges sont attribués aux utilisateurs selon des conditions spécifiques.
 */
    public class Badge {

        // Inner enum for rarity matching DB schema
        public enum Rarete {
            COMMUN, RARE, EPIQUE, LEGENDAIRE
        }

        private int id;
        private String nom;
        private String description;
        private String condition;       // Condition textuelle pour obtenir le badge
        private String conditionType;   // Type de condition (XP, COURS, QUIZ, STREAK, etc.)
        private int conditionValeur;    // Valeur numérique de la condition
        private String icone;           // Emoji ou icône du badge
        private int pointsBonus;        // Points XP bonus attribués avec le badge
        private boolean actif;
        private LocalDateTime createdAt;

        // DB-aligned fields
        private int pointsRequis;       // points_requis in DB
        private int coursRequis;        // cours_requis in DB
        private int niveauRequis;       // niveau_requis in DB
        private String categorie;       // categorie in DB
        private Rarete rarete;          // rarete in DB

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
        this.rarete = Rarete.COMMUN;
        this.categorie = "Général";
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

    // DB-aligned getters/setters

    public String getConditionObtention() {
        return condition;
    }

    public void setConditionObtention(String conditionObtention) {
        this.condition = conditionObtention;
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

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Rarete getRarete() {
        return rarete;
    }

    public void setRarete(Rarete rarete) {
        this.rarete = rarete;
    }

    /**
     * Vérifie si un utilisateur remplit la condition pour obtenir ce badge.
     */
    /**
     * Checks whether this badge's unlock conditions are met.
     * Called by BadgeService.findBadgesEligibles().
     */
    public boolean verifierCondition(int totalXP, int coursCompletes, int niveau) {
        // XP threshold
        if (pointsRequis > 0 && totalXP < pointsRequis) return false;
        // Course completion threshold
        if (coursRequis > 0 && coursCompletes < coursRequis) return false;
        // Level threshold
        if (niveauRequis > 0 && niveau < niveauRequis) return false;

        // Condition-type specific rules
        if (conditionType != null) {
            return switch (conditionType) {
                case CONDITION_XP_TOTAL        -> totalXP       >= conditionValeur;
                case CONDITION_COURS_COMPLETE  -> coursCompletes >= conditionValeur;
                case CONDITION_NIVEAU_ATTEINT  -> niveau         >= conditionValeur;
                case CONDITION_PREMIER_COURS   -> coursCompletes >= 1;
                case CONDITION_CERTIFICATION   -> coursCompletes >= conditionValeur;
                default                        -> true;
            };
        }
        return true;
    }


    /**
     * Retourne l'affichage complet du badge (icône + nom).
     */
    public String getAffichageComplet() {
        return icone + " " + nom;
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
