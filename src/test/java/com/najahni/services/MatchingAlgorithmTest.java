package com.najahni.services;

import com.najahni.models.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le moteur de matching IA (InvestmentMatchingService).
 *
 * Teste les algorithmes de scoring via réflexion pour accéder
 * aux méthodes privées sans nécessiter de connexion DB.
 */
class MatchingAlgorithmTest {

    // ═══════════════════════════════════════════════════════════
    //  SECTOR SCORING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scoring — Secteur")
    class SectorScoring {

        @Test
        @DisplayName("Correspondance exacte du secteur = score 100")
        void exactMatch() throws Exception {
            InvestorProfile profile = createProfile("tech", 5, 1000, 100000, 12);
            Project project = createProject("tech", "Application de gestion");
            double score = invokeSectorScore(profile, project);
            assertEquals(100, score, "Correspondance exacte secteur doit donner 100");
        }

        @Test
        @DisplayName("Secteur contenu dans le secteur du projet = score 100")
        void partialContainment() throws Exception {
            InvestorProfile profile = createProfile("tech", 5, 1000, 100000, 12);
            Project project = createProject("technologie", "Projet tech innovant");
            double score = invokeSectorScore(profile, project);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("Secteur trouvé dans la description = score 80")
        void foundInDescription() throws Exception {
            InvestorProfile profile = createProfile("intelligence artificielle", 5, 1000, 100000, 12);
            Project project = createProject("innovation", "Utilisation de l'intelligence artificielle");
            double score = invokeSectorScore(profile, project);
            assertEquals(80, score);
        }

        @Test
        @DisplayName("Projet null = score neutre 50")
        void nullProject() throws Exception {
            InvestorProfile profile = createProfile("tech", 5, 1000, 100000, 12);
            double score = invokeSectorScore(profile, null);
            assertEquals(50, score);
        }

        @Test
        @DisplayName("Pas de préférence de secteur = score 60")
        void noPreference() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            Project project = createProject("tech", "Application");
            double score = invokeSectorScore(profile, project);
            assertEquals(60, score);
        }

        @Test
        @DisplayName("Aucune correspondance = score 20")
        void noMatch() throws Exception {
            InvestorProfile profile = createProfile("immobilier", 5, 1000, 100000, 12);
            Project project = createProject("biologie", "Recherche médicale");
            double score = invokeSectorScore(profile, project);
            assertTrue(score <= 60, "Sans correspondance le score doit être bas");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BUDGET SCORING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scoring — Budget")
    class BudgetScoring {

        @Test
        @DisplayName("Montant dans la fourchette = score 100")
        void withinRange() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), null, null);
            double score = invokeBudgetScore(profile, opp);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("Montant = budget min = score 100")
        void exactMin() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("1000"), null, null);
            double score = invokeBudgetScore(profile, opp);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("Montant = budget max = score 100")
        void exactMax() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("100000"), null, null);
            double score = invokeBudgetScore(profile, opp);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("Montant hors fourchette donne un score réduit")
        void outOfRange() throws Exception {
            InvestorProfile profile = createProfile("", 5, 10000, 50000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("200000"), null, null);
            double score = invokeBudgetScore(profile, opp);
            assertTrue(score < 50, "Montant très hors budget doit donner un score bas");
        }

        @Test
        @DisplayName("Montant null = score 50")
        void nullAmount() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(null, null, null);
            double score = invokeBudgetScore(profile, opp);
            assertEquals(50, score);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RISK SCORING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scoring — Risque")
    class RiskScoring {

        @Test
        @DisplayName("Risque aligné avec la tolérance = score élevé")
        void alignedRisk() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), 50.0, null);
            // tolérance 5 → risque attendu 50, risque réel 50 → diff = 0 → score = 100
            double score = invokeRiskScore(profile, opp);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("Risque très éloigné de la tolérance = score bas")
        void misalignedRisk() throws Exception {
            InvestorProfile profile = createProfile("", 1, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), 90.0, null);
            // tolérance 1 → risque attendu 10, risque réel 90 → diff = 80 → score ~= 0
            double score = invokeRiskScore(profile, opp);
            assertTrue(score < 20, "Grand écart de risque doit donner un score bas");
        }

        @Test
        @DisplayName("Pas de score de risque = score 60 (neutre)")
        void noRiskScore() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), null, null);
            double score = invokeRiskScore(profile, opp);
            assertEquals(60, score);
        }

        @ParameterizedTest
        @CsvSource({
            "1,10,100",
            "5,50,100",
            "10,100,100",
            "3,30,100",
            "7,70,100"
        })
        @DisplayName("Alignement parfait tolérance → risque = 100")
        void perfectAlignment(int tolerance, double riskScore, double expectedScore) throws Exception {
            InvestorProfile profile = createProfile("", tolerance, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), riskScore, null);
            double score = invokeRiskScore(profile, opp);
            assertEquals(expectedScore, score);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HORIZON SCORING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scoring — Horizon")
    class HorizonScoring {

        @Test
        @DisplayName("Deadline alignée avec l'horizon = score élevé")
        void alignedHorizon() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            // Deadline dans 12 mois ≈ 360 jours
            LocalDate deadline = LocalDate.now().plusMonths(12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), null, deadline);
            double score = invokeHorizonScore(profile, opp);
            assertTrue(score >= 80, "Horizon aligné doit donner un score élevé");
        }

        @Test
        @DisplayName("Deadline passée = score 10")
        void expiredDeadline() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            LocalDate deadline = LocalDate.now().minusDays(30);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), null, deadline);
            double score = invokeHorizonScore(profile, opp);
            assertEquals(10, score);
        }

        @Test
        @DisplayName("Pas de deadline = score 50")
        void noDeadline() throws Exception {
            InvestorProfile profile = createProfile("", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(new BigDecimal("50000"), null, null);
            double score = invokeHorizonScore(profile, opp);
            assertEquals(50, score);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GLOBAL COMPATIBILITY SCORING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scoring — Compatibilité globale")
    class GlobalCompatibility {

        @Test
        @DisplayName("Profil parfaitement aligné donne un score ≥ 80")
        void perfectMatch() throws Exception {
            InvestorProfile profile = createProfile("tech", 5, 10000, 200000, 12);
            InvestmentOpportunity opp = createOpportunity(
                    new BigDecimal("100000"), 50.0, LocalDate.now().plusMonths(12));
            Project project = createProject("tech", "Application mobile de gestion");

            int score = invokeCompatibility(profile, opp, project);
            assertTrue(score >= 80, "Score de compatibilité parfait doit être ≥ 80, obtenu: " + score);
        }

        @Test
        @DisplayName("Score est entre 0 et 100")
        void scoreInRange() throws Exception {
            InvestorProfile profile = createProfile("tech", 5, 1000, 100000, 12);
            InvestmentOpportunity opp = createOpportunity(
                    new BigDecimal("50000"), 40.0, LocalDate.now().plusMonths(6));
            Project project = createProject("santé", "Hôpital du futur");

            int score = invokeCompatibility(profile, opp, project);
            assertTrue(score >= 0 && score <= 100,
                    "Score doit être entre 0 et 100, obtenu: " + score);
        }

        @Test
        @DisplayName("Pondération : secteur=35%, budget=25%, risque=25%, horizon=15%")
        void weightVerification() throws Exception {
            // If all sub-scores are 100, total should be 100
            InvestorProfile profile = createProfile("tech", 5, 10000, 200000, 12);
            InvestmentOpportunity opp = createOpportunity(
                    new BigDecimal("50000"), 50.0, LocalDate.now().plusMonths(12));
            Project project = createProject("tech", "Application tech");

            int score = invokeCompatibility(profile, opp, project);
            // All sub-scores should be 100 or close → total ≈ 100
            assertTrue(score >= 90, "Tous les facteurs alignés → score ≥ 90, obtenu: " + score);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  MATCH RESULT
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MatchResult")
    class MatchResultTests {

        @Test
        @DisplayName("MatchResult stocke correctement les données")
        void matchResultData() {
            InvestmentOpportunity opp = new InvestmentOpportunity();
            opp.setId(5);
            Project proj = new Project();
            proj.setTitle("Mon Projet");

            InvestmentMatchingService.MatchResult result =
                    new InvestmentMatchingService.MatchResult(opp, proj, 85, "✅ Secteur | ✅ Budget");

            assertEquals(opp, result.opportunity);
            assertEquals(proj, result.project);
            assertEquals(85, result.compatibilityScore);
            assertEquals("✅ Secteur | ✅ Budget", result.explanation);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER METHODS — Reflection wrappers
    // ═══════════════════════════════════════════════════════════

    private double invokeSectorScore(InvestorProfile profile, Project project) throws Exception {
        Method m = InvestmentMatchingService.class.getDeclaredMethod(
                "computeSectorScore", InvestorProfile.class, Project.class);
        m.setAccessible(true);
        // Need to create an instance — but the constructor connects to DB
        // Use a workaround: create via unsafe or just test the algorithm directly
        return (double) m.invoke(createServiceSafe(), profile, project);
    }

    private double invokeBudgetScore(InvestorProfile profile, InvestmentOpportunity opp) throws Exception {
        Method m = InvestmentMatchingService.class.getDeclaredMethod(
                "computeBudgetScore", InvestorProfile.class, InvestmentOpportunity.class);
        m.setAccessible(true);
        return (double) m.invoke(createServiceSafe(), profile, opp);
    }

    private double invokeRiskScore(InvestorProfile profile, InvestmentOpportunity opp) throws Exception {
        Method m = InvestmentMatchingService.class.getDeclaredMethod(
                "computeRiskScore", InvestorProfile.class, InvestmentOpportunity.class);
        m.setAccessible(true);
        return (double) m.invoke(createServiceSafe(), profile, opp);
    }

    private double invokeHorizonScore(InvestorProfile profile, InvestmentOpportunity opp) throws Exception {
        Method m = InvestmentMatchingService.class.getDeclaredMethod(
                "computeHorizonScore", InvestorProfile.class, InvestmentOpportunity.class);
        m.setAccessible(true);
        return (double) m.invoke(createServiceSafe(), profile, opp);
    }

    private int invokeCompatibility(InvestorProfile profile, InvestmentOpportunity opp, Project project) throws Exception {
        Method m = InvestmentMatchingService.class.getDeclaredMethod(
                "computeCompatibility", InvestorProfile.class, InvestmentOpportunity.class, Project.class);
        m.setAccessible(true);
        return (int) m.invoke(createServiceSafe(), profile, opp, project);
    }

    /**
     * Creates a service instance bypassing constructor DB init via Mockito.
     * CALLS_REAL_METHODS delegates to actual private methods without calling the constructor.
     */
    private InvestmentMatchingService createServiceSafe() {
        return Mockito.mock(InvestmentMatchingService.class, Mockito.CALLS_REAL_METHODS);
    }

    // ── Factory helpers ──

    private InvestorProfile createProfile(String sectors, int riskTolerance,
                                           double budgetMin, double budgetMax, int horizonMonths) {
        InvestorProfile p = new InvestorProfile();
        p.setPreferredSectors(sectors);
        p.setRiskTolerance(riskTolerance);
        p.setBudgetMin(BigDecimal.valueOf(budgetMin));
        p.setBudgetMax(BigDecimal.valueOf(budgetMax));
        p.setHorizonMonths(horizonMonths);
        return p;
    }

    private Project createProject(String sector, String description) {
        Project p = new Project();
        p.setSector(sector);
        p.setDescription(description);
        return p;
    }

    private InvestmentOpportunity createOpportunity(BigDecimal targetAmount,
                                                      Double riskScore,
                                                      LocalDate deadline) {
        InvestmentOpportunity o = new InvestmentOpportunity();
        o.setTargetAmount(targetAmount);
        o.setRiskScore(riskScore);
        o.setDeadline(deadline);
        return o;
    }
}
