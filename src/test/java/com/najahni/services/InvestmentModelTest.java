package com.najahni.services;

import com.najahni.models.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les modèles d'investissement.
 *
 * Tests purs — aucune dépendance DB ou JavaFX.
 * Couvre : InvestorProfile, InvestorRating, InvestmentContract,
 *          InvestmentOffer, InvestmentOpportunity, Project
 */
class InvestmentModelTest {

    // ═══════════════════════════════════════════════════════════
    //  INVESTOR PROFILE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvestorProfile")
    class InvestorProfileTests {

        @Test
        @DisplayName("Tolérance au risque par défaut = 5")
        void defaultRiskTolerance() {
            InvestorProfile p = new InvestorProfile();
            assertEquals(5, p.getRiskTolerance());
        }

        @Test
        @DisplayName("getSectorArray parse correctement les secteurs CSV")
        void sectorArrayParsingNormal() {
            InvestorProfile p = new InvestorProfile();
            p.setPreferredSectors("tech,santé,énergie");
            String[] sectors = p.getSectorArray();
            assertEquals(3, sectors.length);
            assertEquals("tech", sectors[0]);
            assertEquals("santé", sectors[1]);
            assertEquals("énergie", sectors[2]);
        }

        @Test
        @DisplayName("getSectorArray retourne vide pour null")
        void sectorArrayNull() {
            InvestorProfile p = new InvestorProfile();
            p.setPreferredSectors(null);
            assertEquals(0, p.getSectorArray().length);
        }

        @Test
        @DisplayName("getSectorArray retourne vide pour chaîne vide")
        void sectorArrayEmpty() {
            InvestorProfile p = new InvestorProfile();
            p.setPreferredSectors("   ");
            assertEquals(0, p.getSectorArray().length);
        }

        @Test
        @DisplayName("getSectorArray gère un secteur unique sans virgule")
        void sectorArraySingle() {
            InvestorProfile p = new InvestorProfile();
            p.setPreferredSectors("technologie");
            String[] sectors = p.getSectorArray();
            assertEquals(1, sectors.length);
            assertEquals("technologie", sectors[0]);
        }

        @Test
        @DisplayName("Budget min/max accepte BigDecimal correctement")
        void budgetMinMax() {
            InvestorProfile p = new InvestorProfile();
            p.setBudgetMin(new BigDecimal("1000.50"));
            p.setBudgetMax(new BigDecimal("999999.99"));
            assertEquals(new BigDecimal("1000.50"), p.getBudgetMin());
            assertEquals(new BigDecimal("999999.99"), p.getBudgetMax());
        }

        @Test
        @DisplayName("Horizon en mois se configure correctement")
        void horizonMonths() {
            InvestorProfile p = new InvestorProfile();
            p.setHorizonMonths(24);
            assertEquals(24, p.getHorizonMonths());
        }

        @Test
        @DisplayName("userId et description fonctionnent")
        void userIdAndDescription() {
            InvestorProfile p = new InvestorProfile();
            p.setUserId(42);
            p.setDescription("Investisseur prudent cherchant des opportunités long terme");
            assertEquals(42, p.getUserId());
            assertTrue(p.getDescription().contains("prudent"));
        }

        @Test
        @DisplayName("Timestamps created/updated peuvent être définis")
        void timestamps() {
            InvestorProfile p = new InvestorProfile();
            LocalDateTime now = LocalDateTime.now();
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            assertEquals(now, p.getCreatedAt());
            assertEquals(now, p.getUpdatedAt());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INVESTOR RATING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvestorRating")
    class InvestorRatingTests {

        @Test
        @DisplayName("Constructeur paramétré initialise tous les champs")
        void paramConstructor() {
            InvestorRating r = new InvestorRating(41, 5, 4, "Très bon projet");
            assertEquals(41, r.getInvestorId());
            assertEquals(5, r.getOpportunityId());
            assertEquals(4, r.getRating());
            assertEquals("Très bon projet", r.getComment());
        }

        @Test
        @DisplayName("setRating clamp la valeur entre 1 et 5")
        void ratingClamp() {
            InvestorRating r = new InvestorRating();
            r.setRating(0);
            assertEquals(1, r.getRating(), "Rating < 1 doit être clampé à 1");

            r.setRating(6);
            assertEquals(5, r.getRating(), "Rating > 5 doit être clampé à 5");

            r.setRating(3);
            assertEquals(3, r.getRating(), "Rating valide doit être accepté");
        }

        @Test
        @DisplayName("setRating accepte toutes les valeurs de 1 à 5")
        void allValidRatings() {
            InvestorRating r = new InvestorRating();
            for (int i = 1; i <= 5; i++) {
                r.setRating(i);
                assertEquals(i, r.getRating());
            }
        }

        @ParameterizedTest
        @CsvSource({"1,★☆☆☆☆", "2,★★☆☆☆", "3,★★★☆☆", "4,★★★★☆", "5,★★★★★"})
        @DisplayName("getStarDisplay affiche le bon nombre d'étoiles")
        void starDisplay(int rating, String expected) {
            InvestorRating r = new InvestorRating();
            r.setRating(rating);
            assertEquals(expected, r.getStarDisplay());
        }

        @Test
        @DisplayName("getRatingColor retourne la bonne couleur selon la note")
        void ratingColor() {
            InvestorRating r = new InvestorRating();

            r.setRating(5);
            assertEquals("#27ae60", r.getRatingColor(), "5★ = vert");

            r.setRating(4);
            assertEquals("#27ae60", r.getRatingColor(), "4★ = vert");

            r.setRating(3);
            assertEquals("#f39c12", r.getRatingColor(), "3★ = orange");

            r.setRating(2);
            assertEquals("#e74c3c", r.getRatingColor(), "2★ = rouge");

            r.setRating(1);
            assertEquals("#e74c3c", r.getRatingColor(), "1★ = rouge");
        }

        @Test
        @DisplayName("Champs transient fonctionnent")
        void transientFields() {
            InvestorRating r = new InvestorRating();
            r.setInvestorName("Joe Investor");
            r.setOpportunityDescription("Great opportunity");
            r.setProjectTitle("Project Alpha");

            assertEquals("Joe Investor", r.getInvestorName());
            assertEquals("Great opportunity", r.getOpportunityDescription());
            assertEquals("Project Alpha", r.getProjectTitle());
        }

        @Test
        @DisplayName("Constructeur par défaut ne lance pas d'exception")
        void defaultConstructor() {
            InvestorRating r = new InvestorRating();
            assertNotNull(r);
            assertEquals(0, r.getId());
        }

        @ParameterizedTest
        @ValueSource(ints = {-10, -1, 0})
        @DisplayName("Ratings négatifs ou 0 sont clampés à 1")
        void negativeRatingsClamp(int value) {
            InvestorRating r = new InvestorRating();
            r.setRating(value);
            assertEquals(1, r.getRating());
        }

        @ParameterizedTest
        @ValueSource(ints = {6, 10, 100})
        @DisplayName("Ratings > 5 sont clampés à 5")
        void overflowRatingsClamp(int value) {
            InvestorRating r = new InvestorRating();
            r.setRating(value);
            assertEquals(5, r.getRating());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INVESTMENT OFFER
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvestmentOffer")
    class InvestmentOfferTests {

        @Test
        @DisplayName("Constructeur par défaut : statut = PENDING")
        void defaultPending() {
            InvestmentOffer o = new InvestmentOffer();
            assertEquals(OfferStatus.PENDING, o.getStatus());
        }

        @Test
        @DisplayName("Constructeur sans id initialise les champs")
        void constructorWithoutId() {
            InvestmentOffer o = new InvestmentOffer(
                    new BigDecimal("5000"), OfferStatus.ACCEPTED, 41, 5);
            assertEquals(new BigDecimal("5000"), o.getProposedAmount());
            assertEquals(OfferStatus.ACCEPTED, o.getStatus());
            assertEquals(41, o.getInvestorId());
            assertEquals(5, o.getOpportunityId());
            assertEquals(0, o.getId());
        }

        @Test
        @DisplayName("Constructeur complet avec id")
        void fullConstructor() {
            InvestmentOffer o = new InvestmentOffer(
                    10, new BigDecimal("15000"), OfferStatus.REJECTED, 42, 7);
            assertEquals(10, o.getId());
            assertEquals(new BigDecimal("15000"), o.getProposedAmount());
            assertEquals(OfferStatus.REJECTED, o.getStatus());
        }

        @Test
        @DisplayName("Paiement non effectué par défaut")
        void defaultNotPaid() {
            InvestmentOffer o = new InvestmentOffer();
            assertFalse(o.isPaid());
            assertNull(o.getPaymentIntentId());
            assertNull(o.getPaidAt());
        }

        @Test
        @DisplayName("Paiement marque l'offre comme payée")
        void markAsPaid() {
            InvestmentOffer o = new InvestmentOffer();
            o.setPaid(true);
            o.setPaymentIntentId("pi_test_123");
            o.setPaidAt(LocalDateTime.now());
            assertTrue(o.isPaid());
            assertEquals("pi_test_123", o.getPaymentIntentId());
            assertNotNull(o.getPaidAt());
        }

        @Test
        @DisplayName("Champs transients pour affichage")
        void transientDisplayFields() {
            InvestmentOffer o = new InvestmentOffer();
            o.setInvestorName("Alice Investor");
            o.setProjectTitle("Solar Farm");
            o.setProjectSector("Énergie");
            o.setOpportunityDescription("Panneau solaire 200kW");

            assertEquals("Alice Investor", o.getInvestorName());
            assertEquals("Solar Farm", o.getProjectTitle());
            assertEquals("Énergie", o.getProjectSector());
            assertEquals("Panneau solaire 200kW", o.getOpportunityDescription());
        }

        @Test
        @DisplayName("getFormattedAmount formate correctement")
        void formattedAmount() {
            InvestmentOffer o = new InvestmentOffer();
            o.setProposedAmount(new BigDecimal("25000.50"));
            String result = o.getFormattedAmount();
            assertTrue(result.contains("€"));
        }

        @Test
        @DisplayName("equals et hashCode basés sur l'id")
        void equalsAndHashCode() {
            InvestmentOffer a = new InvestmentOffer();
            a.setId(3);
            InvestmentOffer b = new InvestmentOffer();
            b.setId(3);
            InvestmentOffer c = new InvestmentOffer();
            c.setId(7);
            assertEquals(a, b);
            assertNotEquals(a, c);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("toString contient montant et statut")
        void toStringContent() {
            InvestmentOffer o = new InvestmentOffer(
                    1, new BigDecimal("10000"), OfferStatus.ACCEPTED, 42, 5);
            String s = o.toString();
            assertNotNull(s);
            assertTrue(s.contains("€"), "toString doit contenir le symbole €");
            assertTrue(s.contains("Acceptée"), "toString doit contenir le displayName du statut");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INVESTMENT OPPORTUNITY
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvestmentOpportunity")
    class InvestmentOpportunityTests {

        @Test
        @DisplayName("Constructeur par défaut : statut = OPEN")
        void defaultOpen() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertEquals(OpportunityStatus.OPEN, o.getStatus());
        }

        @Test
        @DisplayName("Constructeur sans id initialise les champs")
        void constructorWithoutId() {
            InvestmentOpportunity o = new InvestmentOpportunity(
                    new BigDecimal("100000"), "Great opportunity",
                    LocalDate.of(2025, 12, 31), OpportunityStatus.OPEN, 16);
            assertEquals(new BigDecimal("100000"), o.getTargetAmount());
            assertEquals("Great opportunity", o.getDescription());
            assertEquals(LocalDate.of(2025, 12, 31), o.getDeadline());
            assertEquals(16, o.getProjectId());
        }

        @Test
        @DisplayName("Score de risque IA est null par défaut")
        void riskScoreNullByDefault() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertNull(o.getRiskScore());
            assertNull(o.getRiskLabel());
        }

        @Test
        @DisplayName("Score de risque peut être défini entre 0 et 100")
        void riskScoreRange() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            o.setRiskScore(45.5);
            assertEquals(45.5, o.getRiskScore());

            o.setRiskLabel("moyen");
            assertEquals("moyen", o.getRiskLabel());
        }

        @Test
        @DisplayName("Titre du projet (transient) fonctionne")
        void projectTitle() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            o.setProjectTitle("Mon Projet Innovant");
            assertEquals("Mon Projet Innovant", o.getProjectTitle());
        }

        @Test
        @DisplayName("getFormattedAmount formate correctement le montant")
        void formattedAmount() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            o.setTargetAmount(new BigDecimal("150000.00"));
            String formatted = o.getFormattedAmount();
            assertTrue(formatted.contains("€"));
            assertFalse(formatted.equals("0,00 €"));
        }

        @Test
        @DisplayName("getFormattedAmount retourne 0,00 € si null")
        void formattedAmountNull() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertEquals("0,00 €", o.getFormattedAmount());
        }

        @Test
        @DisplayName("getRiskLevel calcule correctement le niveau")
        void riskLevel() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertNull(o.getRiskScore());
            assertEquals("—", o.getRiskLevel());

            o.setRiskScore(20.0);
            assertEquals("Faible", o.getRiskLevel());

            o.setRiskScore(50.0);
            assertEquals("Moyen", o.getRiskLevel());

            o.setRiskScore(80.0);
            assertEquals("Élevé", o.getRiskLevel());
        }

        @Test
        @DisplayName("getFormattedRiskScore formate score et emoji")
        void formattedRiskScore() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertEquals("Non calculé", o.getFormattedRiskScore());

            o.setRiskScore(25.0);
            String result = o.getFormattedRiskScore();
            assertTrue(result.contains("25/100"));
            assertTrue(result.contains("Faible"));
            assertTrue(result.contains("🟢"));
        }

        @Test
        @DisplayName("getFormattedRiskLabel formate label ML")
        void formattedRiskLabel() {
            InvestmentOpportunity o = new InvestmentOpportunity();
            assertEquals("Non prédit", o.getFormattedRiskLabel());

            o.setRiskLabel("faible");
            assertEquals("🟢 Faible", o.getFormattedRiskLabel());

            o.setRiskLabel("moyen");
            assertEquals("🟡 Moyen", o.getFormattedRiskLabel());

            o.setRiskLabel("eleve");
            assertEquals("🔴 Élevé", o.getFormattedRiskLabel());
        }

        @Test
        @DisplayName("equals et hashCode basés sur l'id")
        void equalsAndHashCode() {
            InvestmentOpportunity a = new InvestmentOpportunity();
            a.setId(5);
            InvestmentOpportunity b = new InvestmentOpportunity();
            b.setId(5);
            InvestmentOpportunity c = new InvestmentOpportunity();
            c.setId(10);

            assertEquals(a, b);
            assertNotEquals(a, c);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PROJECT
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Project")
    class ProjectTests {

        @Test
        @DisplayName("Constructeur par défaut : statut = DRAFT")
        void defaultDraft() {
            Project p = new Project();
            assertEquals(ProjectStatus.DRAFT, p.getStatus());
        }

        @Test
        @DisplayName("Constructeur sans id initialise les champs")
        void constructorWithoutId() {
            Project p = new Project("Solar Farm", "Panneaux solaires", "Énergie",
                    ProjectStatus.APPROVED, 1);
            assertEquals("Solar Farm", p.getTitle());
            assertEquals("Panneaux solaires", p.getDescription());
            assertEquals("Énergie", p.getSector());
            assertEquals(ProjectStatus.APPROVED, p.getStatus());
            assertEquals(1, p.getEntrepreneurId());
        }

        @Test
        @DisplayName("Constructeur complet avec id")
        void fullConstructor() {
            Project p = new Project(16, "Tech Startup", "Application mobile",
                    "Technologie", ProjectStatus.APPROVED, 42);
            assertEquals(16, p.getId());
            assertEquals("Tech Startup", p.getTitle());
            assertEquals(42, p.getEntrepreneurId());
        }

        @Test
        @DisplayName("entrepreneurId par défaut est 0")
        void defaultEntrepreneurId() {
            Project p = new Project();
            assertEquals(0, p.getEntrepreneurId());
        }

        @Test
        @DisplayName("Nom de l'entrepreneur (transient) fonctionne")
        void entrepreneurName() {
            Project p = new Project();
            p.setEntrepreneurName("Jean Dupont");
            assertEquals("Jean Dupont", p.getEntrepreneurName());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  OFFER STATUS ENUM
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OfferStatus — Enum")
    class OfferStatusTests {

        @Test
        @DisplayName("Contient PENDING, ACCEPTED, REJECTED")
        void containsAllValues() {
            OfferStatus[] values = OfferStatus.values();
            assertTrue(values.length >= 3);
            assertNotNull(OfferStatus.valueOf("PENDING"));
            assertNotNull(OfferStatus.valueOf("ACCEPTED"));
            assertNotNull(OfferStatus.valueOf("REJECTED"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  OPPORTUNITY STATUS ENUM
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OpportunityStatus — Enum")
    class OpportunityStatusTests {

        @Test
        @DisplayName("Contient OPEN, CLOSED, FUNDED")
        void containsAllValues() {
            assertNotNull(OpportunityStatus.valueOf("OPEN"));
            assertNotNull(OpportunityStatus.valueOf("CLOSED"));
            assertNotNull(OpportunityStatus.valueOf("FUNDED"));
        }
    }
}
