package models.apprentissage;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Model représentant un Cours dans le module d'apprentissage NAJAHNI.
 * Un cours peut contenir du contenu vidéo, texte ou quiz.
 */
public class Cours {

    private int id;
    private String titre;
    private String description;
    private String categorie;                  // Catégorie du cours (remplace TypeCours)
    private NiveauCours niveauDifficulte;      // Niveau de difficulté (remplace niveau)
    private int pointsXP;                       // Points XP gagnés en complétant le cours
    private int dureeEstimee;                   // Durée estimée en minutes (remplace dureeMinutes)
    private String imageUrl;                    // URL de l'image de couverture
    private boolean certification;              // Course is certifying
    private boolean actif;                      // Course is active
    private TypeCours type;                     // Type de cours (pour compatibilité UI)
    private int createurId;                     // ID du créateur
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Optional<String> documentPath;
    private Optional<String> videoUrl;

    // Constructeur par défaut
    public Cours() {
        this.niveauDifficulte = NiveauCours.DEBUTANT;
        this.certification = false;
        this.actif = true;
        this.pointsXP = 100;
        this.dureeEstimee = 30;
        this.categorie = "Général";
        this.type = TypeCours.TEXTE;
        this.documentPath = Optional.empty();
        this.videoUrl = Optional.empty();
    }

    // Constructeur sans ID (pour création)
    public Cours(String titre, String description, String categorie, NiveauCours niveauDifficulte, boolean certification) {
        this.titre = titre;
        this.description = description;
        this.categorie = categorie;
        this.niveauDifficulte = niveauDifficulte;
        this.certification = certification;
        this.actif = true;
        this.pointsXP = calculerPointsXPParDefaut(niveauDifficulte);
        this.type = TypeCours.TEXTE;
        this.documentPath = Optional.empty();
        this.videoUrl = Optional.empty();
    }

    // Constructeur complet
    public Cours(int id, String titre, String description, String categorie, NiveauCours niveauDifficulte, 
                 boolean certification, int pointsXP, int dureeEstimee, String documentPath) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.categorie = categorie;
        this.niveauDifficulte = niveauDifficulte;
        this.certification = certification;
        this.pointsXP = pointsXP;
        this.dureeEstimee = dureeEstimee;
        this.actif = true;
        this.type = TypeCours.TEXTE;
        this.documentPath = Optional.ofNullable(documentPath);
        this.videoUrl = Optional.empty();
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

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public NiveauCours getNiveauDifficulte() {
        return niveauDifficulte;
    }

    public void setNiveauDifficulte(NiveauCours niveauDifficulte) {
        this.niveauDifficulte = niveauDifficulte;
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

    public int getDureeEstimee() {
        return dureeEstimee;
    }

    public void setDureeEstimee(int dureeEstimee) {
        this.dureeEstimee = dureeEstimee;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Alias methods for backward compatibility with controllers/services

    public TypeCours getType() {
        return type;
    }

    public void setType(TypeCours type) {
        this.type = type;
    }

    public NiveauCours getNiveau() {
        return niveauDifficulte;
    }

    public void setNiveau(NiveauCours niveau) {
        this.niveauDifficulte = niveau;
    }

    public int getDureeMinutes() {
        return dureeEstimee;
    }

    public void setDureeMinutes(int dureeMinutes) {
        this.dureeEstimee = dureeMinutes;
    }

    public int getCreateurId() {
        return createurId;
    }

    public void setCreateurId(int createurId) {
        this.createurId = createurId;
    }

    public String getTypeIcon() {
        if (type == null) return "[Doc]";
        return switch (type) {
            case VIDEO -> "[Video]";
            case TEXTE -> "[Doc]";
            case QUIZ -> "[Quiz]";
            case MIXTE -> "[Mix]";
        };
    }

    /**
     * Retourne la durée formatée du cours.
     */
    public String getDureeFormatee() {
        if (dureeEstimee < 60) {
            return dureeEstimee + " min";
        } else {
            int heures = dureeEstimee / 60;
            int minutes = dureeEstimee % 60;
            return heures + "h" + (minutes > 0 ? minutes : "");
        }
    }

    /**
     * Retourne l'icône du niveau.
     */
    public String getNiveauIcon() {
        return switch (niveauDifficulte) {
            case DEBUTANT -> "Debutant";
            case INTERMEDIAIRE -> "Intermediaire";
            case AVANCE -> "Avance";
            case EXPERT -> "Expert";
        };
    }

    @Override
    public String toString() {
        return titre + " (" + niveauDifficulte.getDisplayName() + ")";
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

    public Optional<String> getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = Optional.ofNullable(documentPath).filter(s -> !s.isBlank());
    }

    public Optional<String> getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = Optional.ofNullable(videoUrl).filter(s -> !s.isBlank());
    }
}
