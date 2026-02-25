package com.najahni.services;

import com.najahni.models.EconomicData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le module IA d'aide à la décision financière.
 *
 * <h3>Classes testées :</h3>
 * <ul>
 *   <li>{@link EconomicRiskEngine} — Normalisation, scoring, recommandations</li>
 *   <li>{@link EconomicApiService} — Parsing JSON (méthodes statiques)</li>
 *   <li>{@link EconomicData} — Modèle / DTO</li>
 * </ul>
 *
 * <h3>Organisation :</h3>
 * <ol>
 *   <li>Tests de normalisation (taux de change, PIB, inflation, montant, durée)</li>
 *   <li>Tests du scoring composite (facteur économique + score global)</li>
 *   <li>Tests de parsing JSON (exchange rates, World Bank)</li>
 *   <li>Tests du modèle EconomicData</li>
 *   <li>Tests de recommandation et utilitaires</li>
 * </ol>
 */
@DisplayName("Module IA — Aide à la Décision Financière")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EconomicRiskEngineTest {

    private EconomicRiskEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EconomicRiskEngine();
    }

    // ═══════════════════════════════════════════════════════
    //  1. NORMALISATION DU TAUX DE CHANGE
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Normalisation du taux de change EUR/USD")
    class ExchangeRateNormalization {

        @Test
        @Order(1)
        @DisplayName("EUR fort (1.20) → risque faible")
        void strongEur_lowRisk() {
            double factor = engine.normalizeExchangeRate(1.20);
            assertTrue(factor < 50, "EUR fort devrait donner un risque faible, obtenu: " + factor);
        }

        @Test
        @Order(2)
        @DisplayName("EUR à parité (1.10) → risque moyen (~50)")
        void parityEur_mediumRisk() {
            double factor = engine.normalizeExchangeRate(1.10);
            assertTrue(factor >= 40 && factor <= 60,
                "Parité devrait donner risque moyen, obtenu: " + factor);
        }

        @Test
        @Order(3)
        @DisplayName("EUR faible (0.90) → risque élevé")
        void weakEur_highRisk() {
            double factor = engine.normalizeExchangeRate(0.90);
            assertTrue(factor > 50, "EUR faible devrait donner un risque élevé, obtenu: " + factor);
        }

        @Test
        @Order(4)
        @DisplayName("Taux zéro → valeur par défaut (50)")
        void zeroRate_defaultRisk() {
            assertEquals(50.0, engine.normalizeExchangeRate(0));
        }

        @Test
        @Order(5)
        @DisplayName("Taux négatif → valeur par défaut (50)")
        void negativeRate_defaultRisk() {
            assertEquals(50.0, engine.normalizeExchangeRate(-1.0));
        }

        @Test
        @Order(6)
        @DisplayName("Résultat toujours entre 0 et 100")
        void result_clampedBetween0And100() {
            for (double rate = 0.5; rate <= 2.0; rate += 0.1) {
                double factor = engine.normalizeExchangeRate(rate);
                assertTrue(factor >= 0 && factor <= 100,
                    "Facteur hors bornes pour rate=" + rate + ": " + factor);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  2. NORMALISATION DU PIB
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Normalisation du PIB")
    class GdpNormalization {

        @Test
        @Order(10)
        @DisplayName("PIB élevé (> 2000 Mrd) → risque faible")
        void highGdp_lowRisk() {
            double factor = engine.normalizeGdp(2500);
            assertTrue(factor <= 33, "PIB élevé devrait être risque faible, obtenu: " + factor);
        }

        @Test
        @Order(11)
        @DisplayName("PIB moyen (~500 Mrd) → risque moyen")
        void mediumGdp_mediumRisk() {
            double factor = engine.normalizeGdp(500);
            assertTrue(factor > 20 && factor <= 60,
                "PIB moyen devrait être risque modéré, obtenu: " + factor);
        }

        @Test
        @Order(12)
        @DisplayName("PIB faible (< 50 Mrd) → risque élevé")
        void lowGdp_highRisk() {
            double factor = engine.normalizeGdp(20);
            assertTrue(factor >= 60, "PIB faible devrait être risque élevé, obtenu: " + factor);
        }

        @Test
        @Order(13)
        @DisplayName("PIB zéro → risque élevé (80)")
        void zeroGdp_highRisk() {
            assertEquals(80.0, engine.normalizeGdp(0));
        }

        @Test
        @Order(14)
        @DisplayName("PIB négatif → risque élevé (80)")
        void negativeGdp_highRisk() {
            assertEquals(80.0, engine.normalizeGdp(-100));
        }

        @Test
        @Order(15)
        @DisplayName("PIB Tunisie (~46.7 Mrd) → risque élevé")
        void tunisiaGdp_highRisk() {
            double factor = engine.normalizeGdp(46.7);
            assertTrue(factor >= 75 && factor <= 95,
                "PIB Tunisie devrait être risque élevé, obtenu: " + factor);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  3. NORMALISATION DE L'INFLATION
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Normalisation de l'inflation")
    class InflationNormalization {

        @Test
        @Order(20)
        @DisplayName("Inflation idéale (2%) → risque faible (15)")
        void idealInflation_lowRisk() {
            assertEquals(15.0, engine.normalizeInflation(2.0));
        }

        @Test
        @Order(21)
        @DisplayName("Inflation modérée (5%) → risque moyen (~40)")
        void moderateInflation_mediumRisk() {
            double factor = engine.normalizeInflation(5.0);
            assertTrue(factor >= 30 && factor <= 55,
                "Inflation modérée devrait être risque moyen, obtenu: " + factor);
        }

        @Test
        @Order(22)
        @DisplayName("Inflation haute (10%) → risque élevé")
        void highInflation_highRisk() {
            double factor = engine.normalizeInflation(10.0);
            assertTrue(factor >= 65, "Inflation haute devrait être risque élevé, obtenu: " + factor);
        }

        @Test
        @Order(23)
        @DisplayName("Inflation critique (>15%) → risque critique (95)")
        void criticalInflation_criticalRisk() {
            double factor = engine.normalizeInflation(20.0);
            assertEquals(95.0, factor, "Inflation critique devrait être 95");
        }

        @Test
        @Order(24)
        @DisplayName("Déflation (-2%) → risque faible (symétrie)")
        void deflation_lowRisk() {
            double factor = engine.normalizeInflation(-2.0);
            assertEquals(15.0, factor, "Déflation modérée devrait être traitée comme faible");
        }

        @Test
        @Order(25)
        @DisplayName("Inflation Tunisie (~8.3%) → risque élevé")
        void tunisiaInflation() {
            double factor = engine.normalizeInflation(8.3);
            assertTrue(factor >= 55 && factor <= 85,
                "Inflation Tunisie devrait être élevée, obtenu: " + factor);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  4. NORMALISATION DU MONTANT
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Normalisation du montant d'investissement")
    class AmountNormalization {

        @Test
        @Order(30)
        @DisplayName("Petit montant (10 000 €) → risque faible (~2)")
        void smallAmount() {
            double factor = engine.normalizeAmount(new BigDecimal("10000"));
            assertTrue(factor <= 10, "Petit montant devrait être risque faible, obtenu: " + factor);
        }

        @Test
        @Order(31)
        @DisplayName("Montant moyen (250 000 €) → risque moyen (~50)")
        void mediumAmount() {
            double factor = engine.normalizeAmount(new BigDecimal("250000"));
            assertEquals(50.0, factor, 1.0);
        }

        @Test
        @Order(32)
        @DisplayName("Gros montant (500 000 €+) → risque élevé (100)")
        void largeAmount() {
            double factor = engine.normalizeAmount(new BigDecimal("600000"));
            assertEquals(100.0, factor, "Gros montant devrait être clampé à 100");
        }

        @Test
        @Order(33)
        @DisplayName("Montant null → 0")
        void nullAmount() {
            assertEquals(0.0, engine.normalizeAmount(null));
        }

        @Test
        @Order(34)
        @DisplayName("Montant zéro → 0")
        void zeroAmount() {
            assertEquals(0.0, engine.normalizeAmount(BigDecimal.ZERO));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  5. NORMALISATION DE LA DURÉE
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Normalisation de la durée")
    class DurationNormalization {

        @Test
        @Order(40)
        @DisplayName("Deadline dépassée → risque max (100)")
        void pastDeadline_maxRisk() {
            assertEquals(100.0, engine.normalizeDuration(LocalDate.now().minusDays(1)));
        }

        @Test
        @Order(41)
        @DisplayName("Deadline dans 3 jours → risque très élevé (90)")
        void threeDays_veryHighRisk() {
            assertEquals(90.0, engine.normalizeDuration(LocalDate.now().plusDays(3)));
        }

        @Test
        @Order(42)
        @DisplayName("Deadline dans 15 jours → risque élevé (70)")
        void fifteenDays_highRisk() {
            assertEquals(70.0, engine.normalizeDuration(LocalDate.now().plusDays(15)));
        }

        @Test
        @Order(43)
        @DisplayName("Deadline dans 5 mois → risque faible (35)")
        void fiveMonths_lowRisk() {
            assertEquals(35.0, engine.normalizeDuration(LocalDate.now().plusDays(150)));
        }

        @Test
        @Order(44)
        @DisplayName("Deadline dans 2 ans → risque très faible (10)")
        void twoYears_veryLowRisk() {
            assertEquals(10.0, engine.normalizeDuration(LocalDate.now().plusYears(2)));
        }

        @Test
        @Order(45)
        @DisplayName("Deadline null → valeur par défaut (50)")
        void nullDeadline_defaultRisk() {
            assertEquals(50.0, engine.normalizeDuration(null));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  6. FACTEUR ÉCONOMIQUE COMPOSITE
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Facteur économique composite")
    class EconomicFactor {

        @Test
        @Order(50)
        @DisplayName("Données Tunisie réalistes → facteur calculé")
        void tunisiaData_factorCalculated() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            double factor = engine.computeEconomicFactor(data);
            assertTrue(factor >= 0 && factor <= 100,
                "Facteur hors bornes: " + factor);
            assertTrue(factor > 40, "Tunisie devrait avoir un facteur > 40, obtenu: " + factor);
        }

        @Test
        @Order(51)
        @DisplayName("Données France → facteur modéré/faible")
        void franceData_moderateFactor() {
            EconomicData data = new EconomicData(1.08, 3.38, 2780, 4.9, "FR", "France");
            double factor = engine.computeEconomicFactor(data);
            assertTrue(factor < 50, "France devrait avoir un facteur < 50, obtenu: " + factor);
        }

        @Test
        @Order(52)
        @DisplayName("Données null → facteur par défaut (50)")
        void nullData_defaultFactor() {
            assertEquals(50.0, engine.computeEconomicFactor(null));
        }

        @Test
        @Order(53)
        @DisplayName("Données indisponibles → facteur par défaut (50)")
        void unavailableData_defaultFactor() {
            EconomicData data = new EconomicData();
            data.setDataAvailable(false);
            assertEquals(50.0, engine.computeEconomicFactor(data));
        }

        @Test
        @Order(54)
        @DisplayName("Économie excellente → facteur faible")
        void excellentEconomy_lowFactor() {
            EconomicData data = new EconomicData(1.20, 3.38, 5000, 1.5, "US", "USA");
            double factor = engine.computeEconomicFactor(data);
            assertTrue(factor <= 30, "Excellente économie devrait avoir facteur ≤ 30, obtenu: " + factor);
        }

        @Test
        @Order(55)
        @DisplayName("Économie en crise → facteur élevé")
        void crisisEconomy_highFactor() {
            EconomicData data = new EconomicData(0.85, 3.38, 15, 18.0, "XX", "Pays en crise");
            double factor = engine.computeEconomicFactor(data);
            assertTrue(factor >= 70, "Économie en crise devrait avoir facteur ≥ 70, obtenu: " + factor);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  7. SCORE DE RISQUE GLOBAL
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Score de risque global")
    class FullRiskScore {

        @Test
        @Order(60)
        @DisplayName("Calcul complet — petit montant, long terme, bonne économie → faible")
        void lowRisk_scenario() {
            EconomicData data = new EconomicData(1.15, 3.38, 3000, 2.0, "DE", "Allemagne");
            int score = engine.calculateFullRisk(
                new BigDecimal("20000"),
                LocalDate.now().plusYears(1),
                data
            );
            assertTrue(score <= 33, "Scénario faible risque, score=" + score);
        }

        @Test
        @Order(61)
        @DisplayName("Calcul complet — montant moyen, terme moyen, éco moyen → modéré")
        void mediumRisk_scenario() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            int score = engine.calculateFullRisk(
                new BigDecimal("100000"),
                LocalDate.now().plusMonths(3),
                data
            );
            assertTrue(score > 25 && score <= 80,
                "Scénario risque modéré, score=" + score);
        }

        @Test
        @Order(62)
        @DisplayName("Calcul complet — gros montant, court terme, mauvaise éco → élevé")
        void highRisk_scenario() {
            EconomicData data = new EconomicData(0.85, 3.38, 15, 18.0, "XX", "Crise");
            int score = engine.calculateFullRisk(
                new BigDecimal("450000"),
                LocalDate.now().plusDays(10),
                data
            );
            assertTrue(score >= 55, "Scénario haut risque, score=" + score);
        }

        @Test
        @Order(63)
        @DisplayName("Score toujours entre 0 et 100")
        void score_clampedBetween0And100() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            int score = engine.calculateFullRisk(
                new BigDecimal("999999"),
                LocalDate.now().minusDays(30),
                data
            );
            assertTrue(score >= 0 && score <= 100, "Score hors bornes: " + score);
        }

        @Test
        @Order(64)
        @DisplayName("Montant null → IllegalArgumentException")
        void nullAmount_throwsException() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertThrows(IllegalArgumentException.class,
                () -> engine.calculateFullRisk(null, LocalDate.now(), data));
        }

        @Test
        @Order(65)
        @DisplayName("Montant zéro → IllegalArgumentException")
        void zeroAmount_throwsException() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertThrows(IllegalArgumentException.class,
                () -> engine.calculateFullRisk(BigDecimal.ZERO, LocalDate.now(), data));
        }

        @Test
        @Order(66)
        @DisplayName("Données éco null → utilise facteur par défaut (50)")
        void nullEconomicData_usesDefault() {
            int score = engine.calculateFullRisk(
                new BigDecimal("100000"),
                LocalDate.now().plusMonths(6),
                null
            );
            assertTrue(score >= 0 && score <= 100,
                "Score avec données null devrait être valide, obtenu: " + score);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  8. PARSING JSON (EconomicApiService)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Parsing JSON (EconomicApiService)")
    class JsonParsing {

        @Test
        @Order(70)
        @DisplayName("extractJsonDouble — clé existante")
        void extractDouble_existingKey() {
            String json = "{\"rates\":{\"USD\":1.0845,\"TND\":3.3821}}";
            assertEquals(1.0845, EconomicApiService.extractJsonDouble(json, "USD"), 0.001);
        }

        @Test
        @Order(71)
        @DisplayName("extractJsonDouble — clé TND")
        void extractDouble_tndKey() {
            String json = "{\"rates\":{\"USD\":1.0845,\"TND\":3.3821}}";
            assertEquals(3.3821, EconomicApiService.extractJsonDouble(json, "TND"), 0.001);
        }

        @Test
        @Order(72)
        @DisplayName("extractJsonDouble — clé inexistante → RuntimeException")
        void extractDouble_missingKey() {
            String json = "{\"rates\":{\"USD\":1.08}}";
            assertThrows(RuntimeException.class,
                () -> EconomicApiService.extractJsonDouble(json, "GBP"));
        }

        @Test
        @Order(73)
        @DisplayName("extractWorldBankValue — valeur numérique")
        void extractWorldBank_numericValue() {
            String json = "[{\"page\":1},{\"indicator\":{\"id\":\"test\",\"value\":\"GDP\"},\"country\":{\"id\":\"TN\",\"value\":\"Tunisia\"},\"value\":46687214123.456,\"date\":\"2023\"}]";
            double value = EconomicApiService.extractWorldBankValue(json);
            assertEquals(46687214123.456, value, 0.01);
        }

        @Test
        @Order(74)
        @DisplayName("extractWorldBankValue — skip null values")
        void extractWorldBank_skipNullValues() {
            String json = "[{\"page\":1},[{\"value\":null,\"date\":\"2024\"},{\"value\":12345.67,\"date\":\"2023\"}]]";
            double value = EconomicApiService.extractWorldBankValue(json);
            assertEquals(12345.67, value, 0.01);
        }

        @Test
        @Order(75)
        @DisplayName("extractWorldBankDate — extraire l'année")
        void extractWorldBank_date() {
            String json = "[{\"page\":1},[{\"value\":123,\"date\":\"2023\"}]]";
            assertEquals("2023", EconomicApiService.extractWorldBankDate(json));
        }

        @Test
        @Order(76)
        @DisplayName("getCountryName — codes connus")
        void countryName_knownCodes() {
            assertEquals("Tunisie", EconomicApiService.getCountryName("TN"));
            assertEquals("France", EconomicApiService.getCountryName("FR"));
            assertEquals("États-Unis", EconomicApiService.getCountryName("US"));
        }

        @Test
        @Order(77)
        @DisplayName("getCountryName — code inconnu → retourne le code")
        void countryName_unknownCode() {
            assertEquals("ZZ", EconomicApiService.getCountryName("ZZ"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  9. MODÈLE EconomicData
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Modèle EconomicData")
    class EconomicDataModel {

        @Test
        @Order(80)
        @DisplayName("Constructeur par défaut — dataAvailable = false")
        void defaultConstructor_notAvailable() {
            EconomicData data = new EconomicData();
            assertFalse(data.isDataAvailable());
            assertNotNull(data.getFetchTimestamp());
        }

        @Test
        @Order(81)
        @DisplayName("Constructeur complet — dataAvailable = true")
        void fullConstructor_available() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertTrue(data.isDataAvailable());
            assertEquals("TN", data.getCountryCode());
            assertEquals("Tunisie", data.getCountryName());
        }

        @Test
        @Order(82)
        @DisplayName("Formatage PIB")
        void formattedGdp() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertEquals("46.7 Mrd $", data.getFormattedGdp());
        }

        @Test
        @Order(83)
        @DisplayName("Formatage inflation")
        void formattedInflation() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertEquals("8.3%", data.getFormattedInflation());
        }

        @Test
        @Order(84)
        @DisplayName("Formatage EUR/USD")
        void formattedEurUsd() {
            EconomicData data = new EconomicData(1.0845, 3.38, 46.7, 8.3, "TN", "Tunisie");
            assertEquals("1 EUR = 1.0845 USD", data.getFormattedEurUsd());
        }

        @Test
        @Order(85)
        @DisplayName("PIB zéro → N/A")
        void zeroGdp_NA() {
            EconomicData data = new EconomicData();
            assertEquals("N/A", data.getFormattedGdp());
        }

        @Test
        @Order(86)
        @DisplayName("Risk level — faible, modéré, élevé")
        void riskLevels() {
            EconomicData data = new EconomicData();
            data.setEconomicRiskFactor(20);
            assertEquals("Faible", data.getRiskLevel());
            assertEquals("🟢", data.getRiskEmoji());

            data.setEconomicRiskFactor(50);
            assertEquals("Modéré", data.getRiskLevel());
            assertEquals("🟡", data.getRiskEmoji());

            data.setEconomicRiskFactor(80);
            assertEquals("Élevé", data.getRiskLevel());
            assertEquals("🔴", data.getRiskEmoji());
        }

        @Test
        @Order(87)
        @DisplayName("toString contient les informations clés")
        void toStringContainsInfo() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            String str = data.toString();
            assertTrue(str.contains("Tunisie"));
            assertTrue(str.contains("TN"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  10. UTILITAIRES ET RECOMMANDATIONS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("10. Utilitaires et recommandations")
    class UtilitiesAndRecommendations {

        @Test
        @Order(90)
        @DisplayName("getRiskLevel — seuils corrects")
        void riskLevel_thresholds() {
            assertEquals("Faible", EconomicRiskEngine.getRiskLevel(0));
            assertEquals("Faible", EconomicRiskEngine.getRiskLevel(33));
            assertEquals("Modéré", EconomicRiskEngine.getRiskLevel(34));
            assertEquals("Modéré", EconomicRiskEngine.getRiskLevel(66));
            assertEquals("Élevé", EconomicRiskEngine.getRiskLevel(67));
            assertEquals("Élevé", EconomicRiskEngine.getRiskLevel(100));
        }

        @Test
        @Order(91)
        @DisplayName("getRiskEmoji — emojis corrects")
        void riskEmoji_correct() {
            assertEquals("🟢", EconomicRiskEngine.getRiskEmoji(20));
            assertEquals("🟡", EconomicRiskEngine.getRiskEmoji(50));
            assertEquals("🔴", EconomicRiskEngine.getRiskEmoji(80));
        }

        @Test
        @Order(92)
        @DisplayName("getRiskColor — couleurs CSS correctes")
        void riskColor_correct() {
            assertEquals("#27ae60", EconomicRiskEngine.getRiskColor(20));
            assertEquals("#f39c12", EconomicRiskEngine.getRiskColor(50));
            assertEquals("#e74c3c", EconomicRiskEngine.getRiskColor(80));
        }

        @ParameterizedTest
        @Order(93)
        @DisplayName("getRecommendation — toujours non-null")
        @ValueSource(ints = {0, 10, 20, 33, 50, 66, 80, 95, 100})
        void recommendation_neverNull(int score) {
            String rec = EconomicRiskEngine.getRecommendation(score);
            assertNotNull(rec);
            assertFalse(rec.isBlank());
        }

        @Test
        @Order(94)
        @DisplayName("getRecommendation — contient emoji")
        void recommendation_containsEmoji() {
            assertTrue(EconomicRiskEngine.getRecommendation(10).contains("✅"));
            assertTrue(EconomicRiskEngine.getRecommendation(50).contains("⚠️"));
            assertTrue(EconomicRiskEngine.getRecommendation(80).contains("🔴"));
        }

        @Test
        @Order(95)
        @DisplayName("clamp — valeurs aux bornes")
        void clamp_boundaries() {
            assertEquals(0.0, EconomicRiskEngine.clamp(-10, 0, 100));
            assertEquals(100.0, EconomicRiskEngine.clamp(150, 0, 100));
            assertEquals(50.0, EconomicRiskEngine.clamp(50, 0, 100));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  11. TESTS D'INTÉGRATION LÉGERS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("11. Tests d'intégration légers")
    class IntegrationTests {

        @Test
        @Order(100)
        @DisplayName("Scénario Tunisie complet — données → facteur → score")
        void tunisiaFullScenario() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");

            // Compute full risk (also stores economic factor in data)
            int score = engine.calculateFullRisk(
                new BigDecimal("100000"),
                LocalDate.now().plusMonths(6),
                data
            );
            assertTrue(score >= 0 && score <= 100);

            // calculateFullRisk stores economic factor in data
            double storedFactor = data.getEconomicRiskFactor();
            assertTrue(storedFactor > 0 && storedFactor <= 100,
                "Le facteur économique devrait être stocké dans data, obtenu: " + storedFactor);
        }

        @Test
        @Order(101)
        @DisplayName("Scénario France complet — meilleure économie → score plus bas")
        void franceVsTunisia() {
            EconomicData tunisia = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            EconomicData france = new EconomicData(1.08, 3.38, 2780, 4.9, "FR", "France");

            int scoreTN = engine.calculateFullRisk(new BigDecimal("100000"),
                LocalDate.now().plusMonths(6), tunisia);
            int scoreFR = engine.calculateFullRisk(new BigDecimal("100000"),
                LocalDate.now().plusMonths(6), france);

            assertTrue(scoreFR < scoreTN,
                "France devrait avoir un score inférieur à Tunisie (FR="
                + scoreFR + ", TN=" + scoreTN + ")");
        }

        @Test
        @Order(102)
        @DisplayName("Cohérence montant — plus gros montant → score plus élevé")
        void largerAmount_higherScore() {
            EconomicData data = new EconomicData(1.08, 3.38, 46.7, 8.3, "TN", "Tunisie");
            LocalDate deadline = LocalDate.now().plusMonths(6);

            int scoreSmall = engine.calculateFullRisk(new BigDecimal("10000"), deadline, data);
            int scoreLarge = engine.calculateFullRisk(new BigDecimal("400000"), deadline, data);

            assertTrue(scoreLarge >= scoreSmall,
                "Plus gros montant devrait donner score >= (small=" + scoreSmall
                + ", large=" + scoreLarge + ")");
        }

        @Test
        @Order(103)
        @DisplayName("EconomicData — setters et getters cohérents")
        void economicData_gettersSetters() {
            EconomicData data = new EconomicData();
            data.setExchangeRateEurUsd(1.10);
            data.setExchangeRateEurTnd(3.40);
            data.setGdpBillions(50.0);
            data.setInflationRate(7.5);
            data.setCountryCode("TN");
            data.setCountryName("Tunisie");
            data.setDataAvailable(true);
            data.setDataYear("2023");
            data.setEconomicRiskFactor(55.0);

            assertEquals(1.10, data.getExchangeRateEurUsd());
            assertEquals(3.40, data.getExchangeRateEurTnd());
            assertEquals(50.0, data.getGdpBillions());
            assertEquals(7.5, data.getInflationRate());
            assertEquals("TN", data.getCountryCode());
            assertEquals("Tunisie", data.getCountryName());
            assertTrue(data.isDataAvailable());
            assertEquals("2023", data.getDataYear());
            assertEquals(55.0, data.getEconomicRiskFactor());
        }
    }
}
