package com.najahni.services.ml;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entraîneur du modèle de Machine Learning pour la prédiction de risque.
 *
 * <h3>Algorithme : RandomForest</h3>
 * <p>
 * RandomForest est un algorithme d'ensemble (ensemble learning) qui construit
 * plusieurs arbres de décision et combine leurs résultats par vote majoritaire.
 * </p>
 *
 * <h3>Pourquoi RandomForest ?</h3>
 * <ul>
 *   <li><b>Robuste</b> : Résistant au sur-apprentissage (overfitting)</li>
 *   <li><b>Performant</b> : Bonne précision sur données tabulaires mixtes (numériques + nominales)</li>
 *   <li><b>Interprétable</b> : On peut extraire l'importance des attributs</li>
 *   <li><b>Pas de normalisation</b> : Pas besoin de standardiser les données</li>
 *   <li><b>Gère les valeurs manquantes</b> : Nativement supporté par Weka</li>
 * </ul>
 *
 * <h3>Hyperparamètres :</h3>
 * <ul>
 *   <li><b>numIterations</b> = 100 (nombre d'arbres dans la forêt)</li>
 *   <li><b>maxDepth</b> = 0 (profondeur illimitée)</li>
 *   <li><b>seed</b> = 42 (reproductibilité des résultats)</li>
 * </ul>
 *
 * <h3>Processus d'entraînement :</h3>
 * <pre>
 * 1. Charger le dataset ARFF depuis les resources
 * 2. Définir l'attribut classe (risk_label)
 * 3. Configurer RandomForest (100 arbres, seed=42)
 * 4. Entraîner le modèle sur le dataset complet
 * 5. Sauvegarder le modèle binaire (.model)
 * </pre>
 *
 * @see RiskPredictor
 * @see RiskAIService
 */
public class RiskModelTrainer {

    private static final Logger LOGGER = Logger.getLogger(RiskModelTrainer.class.getName());

    /** Chemin du dataset ARFF dans les resources. */
    private static final String DATASET_RESOURCE = "/ml/investment_risk_dataset.arff";

    /** Nom du fichier modèle sauvegardé. */
    private static final String MODEL_FILENAME = "investment_risk.model";

    /** Nombre d'arbres dans la forêt. */
    private static final int NUM_TREES = 100;

    /** Seed pour la reproductibilité. */
    private static final int SEED = 42;

    /**
     * Entraîne le modèle RandomForest et le sauvegarde sur disque.
     *
     * @return Le chemin absolu du fichier modèle sauvegardé.
     * @throws Exception Si le chargement du dataset ou l'entraînement échoue.
     */
    public String trainAndSave() throws Exception {
        LOGGER.info("═══════════════════════════════════════════════");
        LOGGER.info("🤖 ENTRAÎNEMENT DU MODÈLE ML — RandomForest");
        LOGGER.info("═══════════════════════════════════════════════");

        // ── Étape 1 : Charger le dataset ──
        Instances dataset = loadDataset();
        LOGGER.info("📂 Dataset chargé : " + dataset.numInstances() + " instances, "
            + dataset.numAttributes() + " attributs");

        // ── Étape 2 : Définir l'attribut classe ──
        dataset.setClassIndex(dataset.numAttributes() - 1);
        LOGGER.info("🎯 Attribut classe : " + dataset.classAttribute().name()
            + " (" + dataset.numClasses() + " classes)");

        // ── Étape 3 : Configurer RandomForest ──
        RandomForest rf = new RandomForest();
        rf.setNumIterations(NUM_TREES);
        rf.setSeed(SEED);
        LOGGER.info("🌲 Configuration : " + NUM_TREES + " arbres, seed=" + SEED);

        // ── Étape 4 : Entraîner ──
        long startTime = System.currentTimeMillis();
        rf.buildClassifier(dataset);
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("✅ Modèle entraîné en " + duration + " ms");

        // ── Étape 5 : Sauvegarder ──
        String modelPath = saveModel(rf);
        LOGGER.info("💾 Modèle sauvegardé : " + modelPath);
        LOGGER.info("═══════════════════════════════════════════════");

        return modelPath;
    }

    /**
     * Entraîne le modèle et le retourne sans sauvegarder (pour les tests).
     *
     * @return Le classifieur RandomForest entraîné.
     * @throws Exception Si l'entraînement échoue.
     */
    public RandomForest trainModel() throws Exception {
        Instances dataset = loadDataset();
        dataset.setClassIndex(dataset.numAttributes() - 1);

        RandomForest rf = new RandomForest();
        rf.setNumIterations(NUM_TREES);
        rf.setSeed(SEED);
        rf.buildClassifier(dataset);

        return rf;
    }

    /**
     * Charge le dataset ARFF depuis les resources du classpath.
     *
     * @return Les instances Weka chargées.
     * @throws Exception Si le fichier ARFF est introuvable ou invalide.
     */
    public Instances loadDataset() throws Exception {
        InputStream is = getClass().getResourceAsStream(DATASET_RESOURCE);
        if (is == null) {
            throw new RuntimeException("❌ Dataset ARFF introuvable : " + DATASET_RESOURCE);
        }
        DataSource source = new DataSource(is);
        Instances data = source.getDataSet();
        // Définir le dernier attribut comme classe (risk_label)
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }
        LOGGER.info("📊 Dataset : " + data.relationName()
            + " — " + data.numInstances() + " instances"
            + " — classe : " + data.classAttribute().name());
        return data;
    }

    /**
     * Sauvegarde le modèle entraîné dans le dossier resources/ml/.
     *
     * @param model Le classifieur à sauvegarder.
     * @return Le chemin absolu du fichier sauvegardé.
     * @throws Exception Si l'écriture échoue.
     */
    private String saveModel(RandomForest model) throws Exception {
        // Sauvegarder dans le dossier target/classes/ml/ (accessible au classpath)
        Path modelDir = Paths.get("src", "main", "resources", "ml");
        Files.createDirectories(modelDir);
        Path modelPath = modelDir.resolve(MODEL_FILENAME);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(modelPath))) {
            oos.writeObject(model);
        }

        // Copier aussi dans target/classes pour accès runtime immédiat
        Path targetDir = Paths.get("target", "classes", "ml");
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(MODEL_FILENAME);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(targetPath))) {
            oos.writeObject(model);
        }

        return modelPath.toAbsolutePath().toString();
    }
}
