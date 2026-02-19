package com.najahni.services.ml;

import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

/**
 * Prédicteur de risque basé sur le modèle RandomForest entraîné.
 *
 * <h3>Responsabilités :</h3>
 * <ol>
 *   <li>Charger le modèle binaire (.model) depuis les resources</li>
 *   <li>Charger la structure du dataset (pour créer des instances compatibles)</li>
 *   <li>Créer une Instance Weka à partir de valeurs brutes</li>
 *   <li>Prédire le label de risque + la distribution de probabilités</li>
 * </ol>
 *
 * <h3>Utilisation :</h3>
 * <pre>
 * RiskPredictor predictor = new RiskPredictor();
 * predictor.loadModel();
 *
 * RiskPrediction result = predictor.predict(50000, 180, "tech", 60, 0.45);
 * System.out.println(result.getLabel());       // "moyen"
 * System.out.println(result.getProbability());  // 0.82
 * </pre>
 *
 * @see RiskModelTrainer
 * @see RiskPrediction
 */
public class RiskPredictor {

    private static final Logger LOGGER = Logger.getLogger(RiskPredictor.class.getName());

    /** Chemin du modèle sauvegardé dans les resources. */
    private static final String MODEL_RESOURCE = "/ml/investment_risk.model";

    /** Chemin du dataset ARFF (pour la structure des attributs). */
    private static final String DATASET_RESOURCE = "/ml/investment_risk_dataset.arff";

    /** Modèle RandomForest chargé. */
    private RandomForest model;

    /** Structure du dataset (attributs sans données). */
    private Instances datasetStructure;

    /**
     * Charge le modèle et la structure du dataset depuis les resources.
     *
     * @throws Exception Si le modèle ou le dataset n'est pas trouvé.
     */
    public void loadModel() throws Exception {
        LOGGER.info("📂 Chargement du modèle ML...");

        // ── Charger le modèle sérialisé ──
        InputStream modelStream = getClass().getResourceAsStream(MODEL_RESOURCE);
        if (modelStream == null) {
            throw new RuntimeException("❌ Modèle introuvable : " + MODEL_RESOURCE
                + "\n   Veuillez d'abord entraîner le modèle via RiskModelTrainer.trainAndSave()");
        }
        try (ObjectInputStream ois = new ObjectInputStream(modelStream)) {
            model = (RandomForest) ois.readObject();
        }
        LOGGER.info("✅ Modèle RandomForest chargé");

        // ── Charger la structure du dataset ──
        loadDatasetStructure();
    }

    /**
     * Initialise le prédicteur avec un modèle déjà entraîné (pour les tests).
     *
     * @param trainedModel Le modèle RandomForest entraîné.
     * @throws Exception Si le chargement de la structure échoue.
     */
    public void loadModel(RandomForest trainedModel) throws Exception {
        this.model = trainedModel;
        loadDatasetStructure();
        LOGGER.info("✅ Modèle injecté directement (mode test)");
    }

    /**
     * Charge la structure du dataset ARFF (attributs uniquement).
     */
    private void loadDatasetStructure() throws Exception {
        InputStream datasetStream = getClass().getResourceAsStream(DATASET_RESOURCE);
        if (datasetStream == null) {
            throw new RuntimeException("❌ Dataset ARFF introuvable : " + DATASET_RESOURCE);
        }
        DataSource source = new DataSource(datasetStream);
        datasetStructure = source.getDataSet();
        datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);
        // Vider les données (garder uniquement la structure)
        datasetStructure.delete();
        LOGGER.info("📊 Structure dataset chargée : " + datasetStructure.numAttributes() + " attributs");
    }

    /**
     * Prédit le niveau de risque pour une opportunité d'investissement.
     *
     * @param montant     Montant cible en euros.
     * @param duree       Durée restante en jours.
     * @param secteur     Secteur d'activité ("tech", "immobilier", "sante", "industrie").
     * @param successRate Taux de succès historique (0–100).
     * @param fundingRatio Ratio de financement obtenu (0–1).
     * @return {@link RiskPrediction} contenant le label et la probabilité.
     * @throws Exception Si le modèle n'est pas chargé ou la prédiction échoue.
     */
    public RiskPrediction predict(double montant, double duree, String secteur,
                                   double successRate, double fundingRatio) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Le modèle n'est pas chargé. Appelez loadModel() d'abord.");
        }

        LOGGER.info("─────────────────────────────────────────");
        LOGGER.info("🔮 Prédiction ML pour :");
        LOGGER.info("   Montant     = " + montant + " €");
        LOGGER.info("   Durée       = " + duree + " jours");
        LOGGER.info("   Secteur     = " + secteur);
        LOGGER.info("   Success Rate= " + successRate + "%");
        LOGGER.info("   Funding Ratio= " + fundingRatio);

        // ── Créer l'instance Weka ──
        Instance instance = createInstance(montant, duree, secteur, successRate, fundingRatio);

        // ── Prédire ──
        double classIndex = model.classifyInstance(instance);
        double[] distribution = model.distributionForInstance(instance);

        // ── Extraire les résultats ──
        String predictedLabel = datasetStructure.classAttribute().value((int) classIndex);
        double probability = distribution[(int) classIndex];

        // Toutes les probabilités par classe
        String[] classLabels = new String[datasetStructure.numClasses()];
        double[] probabilities = new double[datasetStructure.numClasses()];
        for (int i = 0; i < datasetStructure.numClasses(); i++) {
            classLabels[i] = datasetStructure.classAttribute().value(i);
            probabilities[i] = distribution[i];
        }

        LOGGER.info("📊 Distribution : faible=" + String.format("%.2f", distribution[0])
            + " | moyen=" + String.format("%.2f", distribution[1])
            + " | eleve=" + String.format("%.2f", distribution[2]));
        LOGGER.info("✅ Prédiction : " + predictedLabel.toUpperCase()
            + " (confiance : " + String.format("%.1f%%", probability * 100) + ")");
        LOGGER.info("─────────────────────────────────────────");

        return new RiskPrediction(predictedLabel, probability, classLabels, probabilities);
    }

    /**
     * Crée une Instance Weka compatible avec la structure du dataset.
     *
     * @return Instance Weka prête pour la prédiction.
     */
    private Instance createInstance(double montant, double duree, String secteur,
                                    double successRate, double fundingRatio) {
        // Normaliser le secteur
        String normalizedSecteur = normalizeSecteur(secteur);

        Instance instance = new DenseInstance(datasetStructure.numAttributes());
        instance.setDataset(datasetStructure);

        instance.setValue(0, montant);          // montant
        instance.setValue(1, duree);            // duree
        instance.setValue(2, normalizedSecteur); // secteur (nominal)
        instance.setValue(3, successRate);       // success_rate
        instance.setValue(4, fundingRatio);      // funding_ratio
        // L'attribut classe (index 5) est laissé manquant pour la prédiction

        return instance;
    }

    /**
     * Normalise le nom du secteur vers les valeurs acceptées par le dataset ARFF.
     * Valeurs valides : "tech", "immobilier", "sante", "industrie".
     *
     * @param secteur Secteur brut (peut contenir des accents, majuscules, etc.)
     * @return Secteur normalisé.
     */
    static String normalizeSecteur(String secteur) {
        if (secteur == null || secteur.isBlank()) return "industrie";

        String lower = secteur.toLowerCase().trim();

        // Mapping flexible
        if (lower.contains("tech") || lower.contains("logiciel") || lower.contains("software")
            || lower.contains("informatique") || lower.contains("digital") || lower.contains("ia")) {
            return "tech";
        }
        if (lower.contains("immo") || lower.contains("bâtiment") || lower.contains("batiment")
            || lower.contains("construction") || lower.contains("real estate")) {
            return "immobilier";
        }
        if (lower.contains("sant") || lower.contains("médic") || lower.contains("medic")
            || lower.contains("pharma") || lower.contains("health") || lower.contains("bio")) {
            return "sante";
        }
        if (lower.contains("indust") || lower.contains("manufacture") || lower.contains("usine")
            || lower.contains("énergie") || lower.contains("energie")) {
            return "industrie";
        }

        // Mapping par défaut pour les secteurs connus
        return switch (lower) {
            case "education", "éducation", "formation" -> "tech";
            case "finance", "banque", "assurance" -> "immobilier";
            case "agriculture", "agroalimentaire" -> "industrie";
            default -> "industrie"; // Secteur par défaut
        };
    }

    /** Vérifie si le modèle est chargé. */
    public boolean isModelLoaded() {
        return model != null && datasetStructure != null;
    }
}
