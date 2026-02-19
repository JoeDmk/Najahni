package com.najahni.services.ml;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du module Machine Learning (Weka).
 * 
 * Organisation :
 *   1. Tests de chargement du dataset ARFF
 *   2. Tests d'entraînement du modèle RandomForest
 *   3. Tests de prédiction
 *   4. Tests de cohérence des sorties
 *   5. Tests de RiskAIService (intégration)
 *   6. Tests de normalisation des secteurs
 * 
 * Algorithme : RandomForest (100 arbres, seed=42)
 * Dataset    : 60 instances, 3 classes (faible, moyen, eleve)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🤖 Module ML — Risk Prediction (Weka)")
class RiskMLTest {

    private static RandomForest trainedModel;
    private static Instances datasetStructure;

    // ═══════════════════════════════════════════════════════════
    //  1. DATASET LOADING TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Le dataset ARFF existe et est accessible dans le classpath")
    void testDatasetResourceExists() {
        InputStream is = getClass().getResourceAsStream("/ml/investment_risk_dataset.arff");
        assertNotNull(is, "Le fichier ARFF doit être accessible via le classpath");
    }

    @Test
    @Order(2)
    @DisplayName("Le dataset ARFF se charge correctement avec Weka")
    void testDatasetLoads() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        assertNotNull(dataset, "Le dataset ne doit pas être null");
        assertTrue(dataset.numInstances() > 0, "Le dataset doit contenir des instances");
        assertEquals(6, dataset.numAttributes(), "Le dataset doit avoir 6 attributs");
    }

    @Test
    @Order(3)
    @DisplayName("Le dataset contient exactement 60 instances")
    void testDatasetSize() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        assertEquals(60, dataset.numInstances(), "Le dataset doit contenir 60 instances");
    }

    @Test
    @Order(4)
    @DisplayName("La classe cible est bien 'risk_label' (dernier attribut)")
    void testClassAttribute() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        assertEquals("risk_label", dataset.classAttribute().name(),
            "L'attribut de classe doit être 'risk_label'");
        assertTrue(dataset.classAttribute().isNominal(),
            "L'attribut de classe doit être nominal");
        assertEquals(3, dataset.classAttribute().numValues(),
            "Il doit y avoir 3 classes : faible, moyen, eleve");
    }

    @Test
    @Order(5)
    @DisplayName("Les attributs du dataset ont les bons noms et types")
    void testDatasetAttributes() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        // Attributs numériques
        assertTrue(dataset.attribute("montant").isNumeric());
        assertTrue(dataset.attribute("duree").isNumeric());
        assertTrue(dataset.attribute("success_rate").isNumeric());
        assertTrue(dataset.attribute("funding_ratio").isNumeric());

        // Attributs nominaux
        assertTrue(dataset.attribute("secteur").isNominal());
        assertTrue(dataset.attribute("risk_label").isNominal());
    }

    @Test
    @Order(6)
    @DisplayName("Les 3 classes sont bien présentes : faible, moyen, eleve")
    void testClassValues() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        List<String> expectedClasses = Arrays.asList("faible", "moyen", "eleve");
        for (String cls : expectedClasses) {
            int idx = dataset.classAttribute().indexOfValue(cls);
            assertTrue(idx >= 0, "La classe '" + cls + "' doit exister dans le dataset");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  2. MODEL TRAINING TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("Le modèle RandomForest s'entraîne sans erreur")
    void testModelTrains() throws Exception {
        RiskModelTrainer trainer = new RiskModelTrainer();
        trainedModel = trainer.trainModel();

        assertNotNull(trainedModel, "Le modèle entraîné ne doit pas être null");
    }

    @Test
    @Order(11)
    @DisplayName("Le modèle entraîné est bien un RandomForest")
    void testModelIsRandomForest() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        assertInstanceOf(RandomForest.class, trainedModel);
    }

    @Test
    @Order(12)
    @DisplayName("Le modèle peut classifier au moins une instance du dataset")
    void testModelCanClassify() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        // Essayer de classifier la première instance
        double classIndex = trainedModel.classifyInstance(dataset.instance(0));
        assertTrue(classIndex >= 0 && classIndex < 3,
            "L'index de classe doit être entre 0 et 2");
    }

    @Test
    @Order(13)
    @DisplayName("Le modèle produit des distributions de probabilité valides")
    void testModelDistribution() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskModelTrainer trainer = new RiskModelTrainer();
        Instances dataset = trainer.loadDataset();

        double[] distribution = trainedModel.distributionForInstance(dataset.instance(0));
        assertNotNull(distribution);
        assertEquals(3, distribution.length, "La distribution doit avoir 3 valeurs");

        double sum = 0;
        for (double prob : distribution) {
            assertTrue(prob >= 0, "Chaque probabilité doit être >= 0");
            assertTrue(prob <= 1, "Chaque probabilité doit être <= 1");
            sum += prob;
        }
        assertEquals(1.0, sum, 0.01, "La somme des probabilités doit valoir 1.0");
    }

    // ═══════════════════════════════════════════════════════════
    //  3. PREDICTION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("Le RiskPredictor charge le modèle et prédit sans erreur")
    void testPredictorWorks() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(5000, 365, "tech", 80, 0.9);
        assertNotNull(prediction, "La prédiction ne doit pas être null");
        assertNotNull(prediction.getLabel(), "Le label ne doit pas être null");
        assertFalse(prediction.getLabel().isEmpty(), "Le label ne doit pas être vide");
    }

    @Test
    @Order(21)
    @DisplayName("La prédiction retourne un label valide (faible, moyen, ou eleve)")
    void testPredictionLabelValid() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(5000, 365, "tech", 80, 0.9);
        List<String> validLabels = Arrays.asList("faible", "moyen", "eleve");
        assertTrue(validLabels.contains(prediction.getLabel()),
            "Le label '" + prediction.getLabel() + "' doit être parmi " + validLabels);
    }

    @Test
    @Order(22)
    @DisplayName("La probabilité de la prédiction est entre 0 et 1")
    void testPredictionProbabilityRange() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(50000, 90, "industrie", 30, 0.2);
        assertTrue(prediction.getProbability() >= 0 && prediction.getProbability() <= 1,
            "La probabilité doit être entre 0 et 1, obtenu : " + prediction.getProbability());
    }

    @Test
    @Order(23)
    @DisplayName("Les probabilités détaillées sont cohérentes (somme = 1)")
    void testDetailedProbabilitiesSum() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(20000, 200, "sante", 60, 0.5);
        double[] probs = prediction.getAllProbabilities();
        assertNotNull(probs);
        assertEquals(3, probs.length);

        double sum = Arrays.stream(probs).sum();
        assertEquals(1.0, sum, 0.01, "Somme des probabilités doit être ≈ 1.0");
    }

    @Test
    @Order(24)
    @DisplayName("Les labels détaillés correspondent aux 3 classes")
    void testDetailedLabels() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(20000, 200, "sante", 60, 0.5);
        String[] labels = prediction.getAllLabels();
        assertNotNull(labels);
        assertEquals(3, labels.length);

        List<String> labelList = Arrays.asList(labels);
        assertTrue(labelList.contains("faible"));
        assertTrue(labelList.contains("moyen"));
        assertTrue(labelList.contains("eleve"));
    }

    // ═══════════════════════════════════════════════════════════
    //  4. CONSISTENCY TESTS (même entrée → même sortie)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @DisplayName("Deux prédictions identiques donnent le même résultat (déterminisme seed=42)")
    void testPredictionDeterminism() throws Exception {
        // Entraîner deux modèles séparés avec le même seed
        RandomForest model1 = new RiskModelTrainer().trainModel();
        RandomForest model2 = new RiskModelTrainer().trainModel();

        RiskPredictor pred1 = new RiskPredictor();
        pred1.loadModel(model1);
        RiskPredictor pred2 = new RiskPredictor();
        pred2.loadModel(model2);

        RiskPrediction result1 = pred1.predict(10000, 300, "tech", 70, 0.6);
        RiskPrediction result2 = pred2.predict(10000, 300, "tech", 70, 0.6);

        assertEquals(result1.getLabel(), result2.getLabel(),
            "Le même input doit donner le même label");
        assertEquals(result1.getProbability(), result2.getProbability(), 0.001,
            "Le même input doit donner la même probabilité");
    }

    @ParameterizedTest
    @Order(31)
    @DisplayName("Prédiction stable pour différents secteurs")
    @ValueSource(strings = {"tech", "immobilier", "sante", "industrie"})
    void testPredictionForAllSectors(String secteur) throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        RiskPrediction prediction = predictor.predict(15000, 200, secteur, 55, 0.4);
        assertNotNull(prediction);
        assertNotNull(prediction.getLabel());
        assertTrue(prediction.getProbability() > 0);
    }

    @Test
    @Order(32)
    @DisplayName("Un investissement faible risque est prédit comme tel")
    void testLowRiskPrediction() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        // Petit montant, longue durée, bon taux de succès, bon funding
        RiskPrediction prediction = predictor.predict(3000, 500, "tech", 90, 0.95);
        assertEquals("faible", prediction.getLabel(),
            "Un petit montant avec bons indicateurs devrait être classé 'faible'");
    }

    @Test
    @Order(33)
    @DisplayName("Un investissement haut risque est prédit comme tel")
    void testHighRiskPrediction() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);

        // Gros montant, courte durée, mauvais taux, mauvais funding
        RiskPrediction prediction = predictor.predict(200000, 30, "industrie", 10, 0.05);
        assertEquals("eleve", prediction.getLabel(),
            "Un gros montant avec mauvais indicateurs devrait être classé 'eleve'");
    }

    // ═══════════════════════════════════════════════════════════
    //  5. RISK AI SERVICE (intégration)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @DisplayName("RiskAIService.predictRisk(params) fonctionne en direct")
    void testRiskAIServiceDirect() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);
        RiskAIService service = new RiskAIService(predictor);

        RiskPrediction prediction = service.predictRisk(10000, 180, "tech", 50, 0.4);
        assertNotNull(prediction);
        assertNotNull(prediction.getLabel());
    }

    @Test
    @Order(41)
    @DisplayName("RiskAIService gère tous les secteurs (y compris inconnus)")
    void testRiskAIServiceUnknownSector() throws Exception {
        if (trainedModel == null) {
            trainedModel = new RiskModelTrainer().trainModel();
        }
        RiskPredictor predictor = new RiskPredictor();
        predictor.loadModel(trainedModel);
        RiskAIService service = new RiskAIService(predictor);

        // Secteur inconnu → doit utiliser le défaut (tech)
        RiskPrediction prediction = service.predictRisk(15000, 200, "aerospace", 60, 0.5);
        assertNotNull(prediction);
        List<String> validLabels = Arrays.asList("faible", "moyen", "eleve");
        assertTrue(validLabels.contains(prediction.getLabel()));
    }

    // ═══════════════════════════════════════════════════════════
    //  6. SECTOR NORMALIZATION
    // ═══════════════════════════════════════════════════════════

    @ParameterizedTest
    @Order(50)
    @DisplayName("Normalisation des secteurs (mapping vers valeurs ARFF)")
    @CsvSource({
        "tech, tech",
        "Technology, tech",
        "Technologie, tech",
        "immobilier, immobilier",
        "Real Estate, immobilier",
        "sante, sante",
        "Santé, sante",
        "Health, sante",
        "industrie, industrie",
        "Industry, industrie",
        "Manufacturing, industrie",
        "unknown_sector, industrie"
    })
    void testSectorNormalization(String input, String expected) {
        String result = RiskPredictor.normalizeSecteur(input);
        assertEquals(expected, result,
            "Le secteur '" + input + "' doit être normalisé en '" + expected + "'");
    }

    // ═══════════════════════════════════════════════════════════
    //  7. RISK PREDICTION DTO DISPLAY
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(60)
    @DisplayName("RiskPrediction.getDisplay() retourne un format correct")
    void testPredictionDisplay() {
        RiskPrediction prediction = new RiskPrediction(
            "faible", 0.85,
            new String[]{"faible", "moyen", "eleve"},
            new double[]{0.85, 0.10, 0.05}
        );

        String display = prediction.getDisplay();
        assertNotNull(display);
        assertTrue(display.contains("Faible"), "L'affichage doit contenir 'Faible'");
        assertTrue(display.contains("🟢"), "L'affichage doit contenir l'emoji vert");
        assertTrue(display.contains("85"), "L'affichage doit contenir le pourcentage");
    }

    @Test
    @Order(61)
    @DisplayName("RiskPrediction.getDisplayLabel() pour chaque classe")
    void testDisplayLabels() {
        assertEquals("Faible", new RiskPrediction("faible", 0.9, null, null).getDisplayLabel());
        assertEquals("Moyen", new RiskPrediction("moyen", 0.7, null, null).getDisplayLabel());
        assertEquals("Élevé", new RiskPrediction("eleve", 0.8, null, null).getDisplayLabel());
    }

    @Test
    @Order(62)
    @DisplayName("RiskPrediction.getEmoji() pour chaque classe")
    void testEmojis() {
        assertEquals("🟢", new RiskPrediction("faible", 0.9, null, null).getEmoji());
        assertEquals("🟡", new RiskPrediction("moyen", 0.7, null, null).getEmoji());
        assertEquals("🔴", new RiskPrediction("eleve", 0.8, null, null).getEmoji());
    }

    @Test
    @Order(63)
    @DisplayName("RiskPrediction.getProbabilityPercent() est correct")
    void testProbabilityPercent() {
        RiskPrediction prediction = new RiskPrediction("faible", 0.853, null, null);
        assertEquals(85.3, prediction.getProbabilityPercent(), 0.1);
    }

    @Test
    @Order(64)
    @DisplayName("RiskPrediction.getDetailedDisplay() contient les 3 classes")
    void testDetailedDisplay() {
        RiskPrediction prediction = new RiskPrediction(
            "moyen", 0.60,
            new String[]{"faible", "moyen", "eleve"},
            new double[]{0.25, 0.60, 0.15}
        );

        String detailed = prediction.getDetailedDisplay();
        assertNotNull(detailed);
        assertTrue(detailed.contains("faible") || detailed.contains("Faible"));
        assertTrue(detailed.contains("moyen") || detailed.contains("Moyen"));
        assertTrue(detailed.contains("eleve") || detailed.contains("Élevé"));
    }
}
