package com.najahni.services.ml;

import com.najahni.models.InvestmentOpportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service IA qui orchestre la prédiction de risque ML pour les opportunités.
 *
 * <h3>Responsabilités :</h3>
 * <ol>
 *   <li>Transformer une entité {@link InvestmentOpportunity} en vecteur de features Weka</li>
 *   <li>Charger le modèle entraîné via {@link RiskPredictor}</li>
 *   <li>Appeler la prédiction et retourner un {@link RiskPrediction}</li>
 * </ol>
 *
 * <h3>Mapping Entité → Features ML :</h3>
 * <pre>
 * InvestmentOpportunity      →   Instance Weka
 * ─────────────────────           ──────────────
 * targetAmount               →   montant (numeric)
 * deadline - now()            →   duree (numeric, jours)
 * project.sector              →   secteur (nominal)
 * (dérivé / défaut 50)        →   success_rate (numeric)
 * (dérivé / défaut 0.0)       →   funding_ratio (numeric)
 * </pre>
 *
 * <h3>Diagramme de séquence :</h3>
 * <pre>
 * Controller
 *   │
 *   ├─→ RiskAIService.predictRisk(opportunity, sector)
 *   │       │
 *   │       ├─→ Extraire features (montant, durée, secteur...)
 *   │       │
 *   │       ├─→ RiskPredictor.predict(montant, duree, secteur, ...)
 *   │       │       ├─→ Créer Instance Weka
 *   │       │       ├─→ RandomForest.classifyInstance()
 *   │       │       └─→ return RiskPrediction(label, probability)
 *   │       │
 *   │       └─→ return RiskPrediction
 *   │
 *   ├─→ InvestmentOpportunityService.updateRiskLabel(id, label)
 *   │       └─→ UPDATE SQL
 *   │
 *   └─→ Afficher résultat dans popup
 * </pre>
 *
 * @see RiskPredictor
 * @see RiskPrediction
 * @see RiskModelTrainer
 */
public class RiskAIService {

    private static final Logger LOGGER = Logger.getLogger(RiskAIService.class.getName());

    private final RiskPredictor predictor;
    private boolean modelLoaded = false;

    /** Constructeur par défaut. */
    public RiskAIService() {
        this.predictor = new RiskPredictor();
    }

    /** Constructeur pour tests (injection du prédicteur). */
    public RiskAIService(RiskPredictor predictor) {
        this.predictor = predictor;
        this.modelLoaded = predictor.isModelLoaded();
    }

    /**
     * Initialise le service en chargeant le modèle.
     * Si le modèle n'existe pas, il entraîne automatiquement un nouveau modèle.
     */
    public void initialize() {
        try {
            predictor.loadModel();
            modelLoaded = true;
            LOGGER.info("✅ RiskAIService initialisé — modèle ML chargé");
        } catch (Exception e) {
            LOGGER.warning("⚠️ Modèle non trouvé, entraînement automatique...");
            try {
                // Entraîner et sauvegarder le modèle
                RiskModelTrainer trainer = new RiskModelTrainer();
                trainer.trainAndSave();

                // Recharger
                predictor.loadModel();
                modelLoaded = true;
                LOGGER.info("✅ Modèle entraîné et chargé automatiquement");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "❌ Impossible d'initialiser le service ML", ex);
                modelLoaded = false;
            }
        }
    }

    /**
     * Prédit le niveau de risque pour une opportunité d'investissement.
     *
     * @param opportunity L'opportunité à évaluer. Ne doit pas être null.
     * @param projectSector Le secteur du projet associé (peut être null → "industrie").
     * @return {@link RiskPrediction} avec le label, la probabilité et la distribution.
     * @throws Exception Si la prédiction échoue.
     */
    public RiskPrediction predictRisk(InvestmentOpportunity opportunity, String projectSector) throws Exception {
        if (opportunity == null) {
            throw new IllegalArgumentException("L'opportunité ne peut pas être null.");
        }
        if (!modelLoaded) {
            initialize();
            if (!modelLoaded) {
                throw new IllegalStateException("Le modèle ML n'est pas disponible.");
            }
        }

        LOGGER.info("═══════════════════════════════════════════════");
        LOGGER.info("🤖 PRÉDICTION ML pour Opportunité #" + opportunity.getId());
        LOGGER.info("═══════════════════════════════════════════════");

        // ── Extraction des features ──
        double montant = extractMontant(opportunity);
        double duree = extractDuree(opportunity);
        String secteur = projectSector != null ? projectSector : "industrie";
        double successRate = extractSuccessRate(opportunity);
        double fundingRatio = extractFundingRatio(opportunity);

        LOGGER.info("📋 Features extraites :");
        LOGGER.info("   montant      = " + montant);
        LOGGER.info("   duree        = " + duree);
        LOGGER.info("   secteur      = " + secteur);
        LOGGER.info("   success_rate = " + successRate);
        LOGGER.info("   funding_ratio= " + fundingRatio);

        // ── Prédiction ──
        RiskPrediction prediction = predictor.predict(montant, duree, secteur, successRate, fundingRatio);

        LOGGER.info("🎯 Résultat : " + prediction.getDisplay());
        LOGGER.info("═══════════════════════════════════════════════");

        return prediction;
    }

    /**
     * Prédit le risque avec tous les paramètres explicites (pour tests ou usage avancé).
     */
    public RiskPrediction predictRisk(double montant, double duree, String secteur,
                                       double successRate, double fundingRatio) throws Exception {
        if (!modelLoaded) {
            initialize();
            if (!modelLoaded) {
                throw new IllegalStateException("Le modèle ML n'est pas disponible.");
            }
        }
        return predictor.predict(montant, duree, secteur, successRate, fundingRatio);
    }

    // ─── EXTRACTION DES FEATURES ─────────────────────────────

    /**
     * Extrait le montant de l'opportunité.
     * Si null ou zéro, retourne 10000 (valeur par défaut neutre).
     */
    private double extractMontant(InvestmentOpportunity opp) {
        if (opp.getTargetAmount() == null) return 10000.0;
        return opp.getTargetAmount().doubleValue();
    }

    /**
     * Calcule la durée restante en jours entre maintenant et la deadline.
     * Si la deadline est null ou passée, retourne 30 (valeur par défaut).
     */
    private double extractDuree(InvestmentOpportunity opp) {
        if (opp.getDeadline() == null) return 180.0; // 6 mois par défaut
        long days = ChronoUnit.DAYS.between(LocalDate.now(), opp.getDeadline());
        return Math.max(1, days); // Minimum 1 jour
    }

    /**
     * Estime le taux de succès du projet.
     * Dans cette version, on utilise une heuristique basée sur le statut.
     */
    private double extractSuccessRate(InvestmentOpportunity opp) {
        // Heuristique : statut FUNDED = bon taux, OPEN = moyen, CLOSED = faible
        return switch (opp.getStatus()) {
            case FUNDED -> 70.0;
            case OPEN   -> 50.0;
            case CLOSED -> 30.0;
        };
    }

    /**
     * Estime le ratio de financement.
     * Dans cette version, on utilise une heuristique basée sur le statut.
     */
    private double extractFundingRatio(InvestmentOpportunity opp) {
        return switch (opp.getStatus()) {
            case FUNDED -> 0.80;
            case OPEN   -> 0.20;
            case CLOSED -> 0.10;
        };
    }

    /** Vérifie si le modèle est chargé et prêt. */
    public boolean isReady() {
        return modelLoaded;
    }
}
