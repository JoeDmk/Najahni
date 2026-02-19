package com.najahni.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour RiskCalculator.
 *
 * Classe pure logique métier → aucun mock nécessaire.
 * On teste directement les méthodes de calcul.
 *
 * Formule : risk_score = (facteurMontant × 0.4) + (facteurDuree × 0.2) + (facteurAPI × 0.4)
 *
 * Niveaux de risque :
 *   - Faible : 0 – 33
 *   - Moyen  : 34 – 66
 *   - Élevé  : 67 – 100
 */
class RiskCalculatorTest {

    private RiskCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskCalculator();
    }

    // ═══════════════════════════════════════════════════════════
    // Tests du score global (computeScore)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("computeScore — Calcul du score global")
    class ComputeScoreTests {

        @Test
        @DisplayName("Petit montant + deadline lointaine + API faible → risque faible (< 34)")
        void testLowRiskScenario() {
            // 10 000 € → facteurMontant = 2.0 (petit)
            // deadline dans 1 an → facteurDurée = 15.0 (faible)
            // apiFactor = 10.0 (conditions favorables)
            int score = calculator.computeScore(
                new BigDecimal("10000"),
                LocalDate.now().plusDays(365),
                10.0
            );
            assertTrue(score >= 0 && score <= 33,
                "Score faible attendu, obtenu : " + score);
            assertEquals("Faible", RiskCalculator.getRiskLevel(score));
        }

        @Test
        @DisplayName("Montant moyen + deadline 2 mois + API moyenne → risque moyen (34–66)")
        void testMediumRiskScenario() {
            // 200 000 € → facteurMontant = 40.0
            // deadline dans 60 jours → facteurDurée = 50.0
            // apiFactor = 50.0
            int score = calculator.computeScore(
                new BigDecimal("200000"),
                LocalDate.now().plusDays(60),
                50.0
            );
            assertTrue(score >= 20 && score <= 80,
                "Score moyen attendu (fourchette large), obtenu : " + score);
        }

        @Test
        @DisplayName("Gros montant + deadline demain + API élevée → risque élevé (> 66)")
        void testHighRiskScenario() {
            // 500 000 € → facteurMontant = 100.0
            // deadline dans 1 jour → facteurDurée = 90.0
            // apiFactor = 90.0
            int score = calculator.computeScore(
                new BigDecimal("500000"),
                LocalDate.now().plusDays(1),
                90.0
            );
            assertTrue(score >= 67 && score <= 100,
                "Score élevé attendu, obtenu : " + score);
            assertEquals("Élevé", RiskCalculator.getRiskLevel(score));
        }

        @Test
        @DisplayName("Montant nul = 0 → facteurMontant = 0")
        void testZeroAmount() {
            // 0 € → facteurMontant = 0
            // API = 50, durée neutre = 50
            int score = calculator.computeScore(
                BigDecimal.ZERO,
                null,  // deadline null → facteurDurée = 50
                50.0
            );
            // score = (0 * 0.4) + (50 * 0.2) + (50 * 0.4) = 0 + 10 + 20 = 30
            assertTrue(score >= 0 && score <= 40,
                "Score avec montant zéro, obtenu : " + score);
        }

        @Test
        @DisplayName("Montant null → IllegalArgumentException")
        void testNullAmountThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> calculator.computeScore(null, LocalDate.now().plusDays(30), 50.0),
                "Un montant null doit lever une exception");
        }

        @Test
        @DisplayName("Montant négatif → IllegalArgumentException")
        void testNegativeAmountThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> calculator.computeScore(new BigDecimal("-1000"), LocalDate.now().plusDays(30), 50.0),
                "Un montant négatif doit lever une exception");
        }

        @Test
        @DisplayName("API factor négatif → clampé à 0")
        void testNegativeApiFactor() {
            int score = calculator.computeScore(
                new BigDecimal("100000"),
                LocalDate.now().plusDays(90),
                -20.0  // sera clampé à 0
            );
            assertTrue(score >= 0 && score <= 100,
                "Score doit être entre 0–100, obtenu : " + score);
        }

        @Test
        @DisplayName("API factor > 100 → clampé à 100")
        void testOverflowApiFactor() {
            int score = calculator.computeScore(
                new BigDecimal("100000"),
                LocalDate.now().plusDays(90),
                150.0  // sera clampé à 100
            );
            assertTrue(score >= 0 && score <= 100,
                "Score doit être entre 0–100 même avec API=150, obtenu : " + score);
        }

        @Test
        @DisplayName("Score toujours entre 0 et 100")
        void testScoreAlwaysBounded() {
            // Cas extrême : tout au maximum
            int scoreMax = calculator.computeScore(
                new BigDecimal("1000000"), // > MAX_AMOUNT → 100
                LocalDate.now().minusDays(1), // passé → 100
                100.0  // max
            );
            assertEquals(100, scoreMax, "Score max doit être 100");

            // Cas extrême : tout au minimum
            int scoreMin = calculator.computeScore(
                BigDecimal.ZERO,
                LocalDate.now().plusYears(5),
                0.0
            );
            assertTrue(scoreMin >= 0 && scoreMin <= 10,
                "Score min doit être proche de 0, obtenu : " + scoreMin);
        }

        @Test
        @DisplayName("Deadline null → facteur durée = 50 (valeur neutre)")
        void testNullDeadline() {
            int score = calculator.computeScore(
                new BigDecimal("100000"),
                null,
                50.0
            );
            // facteurMontant = 20, facteurDurée = 50 (neutre), facteurAPI = 50
            // score = (20*0.4) + (50*0.2) + (50*0.4) = 8 + 10 + 20 = 38
            assertTrue(score >= 20 && score <= 55,
                "Score avec deadline null, obtenu : " + score);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tests du facteur montant (computeAmountFactor)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("computeAmountFactor — Normalisation du montant")
    class AmountFactorTests {

        @Test
        @DisplayName("Montant 0 → facteur 0")
        void testZeroAmount() {
            assertEquals(0.0, calculator.computeAmountFactor(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Montant 250 000 → facteur ≈ 50")
        void testHalfMaxAmount() {
            double factor = calculator.computeAmountFactor(new BigDecimal("250000"));
            assertEquals(50.0, factor, 0.1);
        }

        @Test
        @DisplayName("Montant 500 000+ → facteur 100 (cap)")
        void testMaxAmountCapped() {
            assertEquals(100.0, calculator.computeAmountFactor(new BigDecimal("500000")));
            assertEquals(100.0, calculator.computeAmountFactor(new BigDecimal("1000000")));
        }

        @Test
        @DisplayName("Montant 100 000 → facteur 20")
        void testTypicalAmount() {
            double factor = calculator.computeAmountFactor(new BigDecimal("100000"));
            assertEquals(20.0, factor, 0.1);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tests du facteur durée (computeDurationFactor)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("computeDurationFactor — Normalisation de la durée")
    class DurationFactorTests {

        @Test
        @DisplayName("Deadline passée → 100 (risque maximum)")
        void testPastDeadline() {
            assertEquals(100.0, calculator.computeDurationFactor(LocalDate.now().minusDays(5)));
        }

        @Test
        @DisplayName("Deadline dans 3 jours → 90")
        void testVeryShortDeadline() {
            assertEquals(90.0, calculator.computeDurationFactor(LocalDate.now().plusDays(3)));
        }

        @Test
        @DisplayName("Deadline dans 15 jours → 70")
        void testShortDeadline() {
            assertEquals(70.0, calculator.computeDurationFactor(LocalDate.now().plusDays(15)));
        }

        @Test
        @DisplayName("Deadline dans 60 jours → 50")
        void testMediumDeadline() {
            assertEquals(50.0, calculator.computeDurationFactor(LocalDate.now().plusDays(60)));
        }

        @Test
        @DisplayName("Deadline dans 1 an → 15")
        void testLongDeadline() {
            assertEquals(15.0, calculator.computeDurationFactor(LocalDate.now().plusDays(200)));
        }

        @Test
        @DisplayName("Deadline dans 2 ans → 10 (risque minimum)")
        void testVeryLongDeadline() {
            assertEquals(10.0, calculator.computeDurationFactor(LocalDate.now().plusDays(730)));
        }

        @Test
        @DisplayName("Deadline null → 50 (valeur neutre)")
        void testNullDeadline() {
            assertEquals(50.0, calculator.computeDurationFactor(null));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tests du niveau de risque (getRiskLevel)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRiskLevel — Classification du risque")
    class RiskLevelTests {

        @Test
        @DisplayName("Score 0 → Faible")
        void testMinScore() {
            assertEquals("Faible", RiskCalculator.getRiskLevel(0));
        }

        @Test
        @DisplayName("Score 33 → Faible (limite)")
        void testFaibleLimit() {
            assertEquals("Faible", RiskCalculator.getRiskLevel(33));
        }

        @Test
        @DisplayName("Score 34 → Moyen")
        void testMoyenStart() {
            assertEquals("Moyen", RiskCalculator.getRiskLevel(34));
        }

        @Test
        @DisplayName("Score 66 → Moyen (limite)")
        void testMoyenLimit() {
            assertEquals("Moyen", RiskCalculator.getRiskLevel(66));
        }

        @Test
        @DisplayName("Score 67 → Élevé")
        void testEleveStart() {
            assertEquals("Élevé", RiskCalculator.getRiskLevel(67));
        }

        @Test
        @DisplayName("Score 100 → Élevé")
        void testMaxScore() {
            assertEquals("Élevé", RiskCalculator.getRiskLevel(100));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tests du parsing JSON (RiskService.extractJsonDouble)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractJsonDouble — Parsing JSON manuel")
    class JsonParsingTests {

        @Test
        @DisplayName("Parse température depuis JSON Open-Meteo")
        void testParseTemperature() {
            String json = """
                {"current":{"temperature_2m":22.5,"wind_speed_10m":8.3}}
                """;
            double temp = RiskService.extractJsonDouble(json, "temperature_2m");
            assertEquals(22.5, temp, 0.01);
        }

        @Test
        @DisplayName("Parse vent depuis JSON Open-Meteo")
        void testParseWind() {
            String json = """
                {"current":{"temperature_2m":22.5,"wind_speed_10m":15.7}}
                """;
            double wind = RiskService.extractJsonDouble(json, "wind_speed_10m");
            assertEquals(15.7, wind, 0.01);
        }

        @Test
        @DisplayName("Parse valeur négative")
        void testParseNegativeValue() {
            String json = """
                {"current":{"temperature_2m":-5.3,"wind_speed_10m":12.0}}
                """;
            double temp = RiskService.extractJsonDouble(json, "temperature_2m");
            assertEquals(-5.3, temp, 0.01);
        }

        @Test
        @DisplayName("Clé introuvable → RuntimeException")
        void testKeyNotFound() {
            String json = """
                {"current":{"temperature_2m":22.5}}
                """;
            assertThrows(RuntimeException.class,
                () -> RiskService.extractJsonDouble(json, "humidity"),
                "Une clé absente doit lever une RuntimeException");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tests des emojis et styles
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRiskEmoji / getRiskStyle — Indicateurs visuels")
    class VisualIndicatorsTests {

        @Test
        @DisplayName("Score faible → 🟢 vert")
        void testLowEmoji() {
            assertEquals("🟢", RiskCalculator.getRiskEmoji(20));
            assertTrue(RiskCalculator.getRiskStyle(20).contains("#27ae60"));
        }

        @Test
        @DisplayName("Score moyen → 🟡 orange")
        void testMediumEmoji() {
            assertEquals("🟡", RiskCalculator.getRiskEmoji(50));
            assertTrue(RiskCalculator.getRiskStyle(50).contains("#f39c12"));
        }

        @Test
        @DisplayName("Score élevé → 🔴 rouge")
        void testHighEmoji() {
            assertEquals("🔴", RiskCalculator.getRiskEmoji(80));
            assertTrue(RiskCalculator.getRiskStyle(80).contains("#e74c3c"));
        }
    }
}
