package com.najahni.services;

import com.najahni.models.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ContractService.
 *
 * Teste les fonctions pures (SHA-256, génération de termes, vérification d'intégrité)
 * sans dépendance à la base de données.
 */
class ContractServiceTest {

    // ═══════════════════════════════════════════════════════════
    //  SHA-256 HASHING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SHA-256 Hashing")
    class Sha256Tests {

        @Test
        @DisplayName("sha256 produit un hash de 64 caractères hexadécimaux")
        void sha256ProducesCorrectLength() {
            String hash = ContractService.sha256("test input");
            assertNotNull(hash);
            assertEquals(64, hash.length(), "SHA-256 hash doit avoir 64 caractères hex");
        }

        @Test
        @DisplayName("sha256 est déterministe — même entrée = même hash")
        void sha256IsDeterministic() {
            String input = "NAJAHNI-2025-000001|1|42|5000|10|16|terms";
            String hash1 = ContractService.sha256(input);
            String hash2 = ContractService.sha256(input);
            assertEquals(hash1, hash2, "Même entrée doit produire le même hash");
        }

        @Test
        @DisplayName("sha256 diffère pour des entrées différentes")
        void sha256DiffersForDifferentInputs() {
            String hash1 = ContractService.sha256("input A");
            String hash2 = ContractService.sha256("input B");
            assertNotEquals(hash1, hash2, "Entrées différentes doivent produire des hash différents");
        }

        @Test
        @DisplayName("sha256 gère la chaîne vide")
        void sha256HandlesEmptyString() {
            String hash = ContractService.sha256("");
            assertNotNull(hash);
            assertEquals(64, hash.length());
            // SHA-256 de "" = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
        }

        @Test
        @DisplayName("sha256 gère les caractères Unicode/accents")
        void sha256HandlesUnicode() {
            String hash = ContractService.sha256("résumé éléphant café");
            assertNotNull(hash);
            assertEquals(64, hash.length());
        }

        @Test
        @DisplayName("sha256 gère les chaînes très longues")
        void sha256HandlesLongStrings() {
            String longInput = "a".repeat(10000);
            String hash = ContractService.sha256(longInput);
            assertNotNull(hash);
            assertEquals(64, hash.length());
        }

        @Test
        @DisplayName("sha256 ne contient que des caractères hexadécimaux")
        void sha256OnlyHexChars() {
            String hash = ContractService.sha256("test");
            assertTrue(hash.matches("[0-9a-f]{64}"), "Hash doit être en hexadécimal minuscule");
        }

        @Test
        @DisplayName("sha256 est sensible à la casse")
        void sha256IsCaseSensitive() {
            String lower = ContractService.sha256("Test");
            String upper = ContractService.sha256("test");
            assertNotEquals(lower, upper);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CONTRACT MODEL
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvestmentContract — Modèle")
    class ContractModelTests {

        @Test
        @DisplayName("Contrat par défaut a le statut DRAFT")
        void defaultStatusIsDraft() {
            InvestmentContract contract = new InvestmentContract();
            assertEquals(ContractStatus.DRAFT, contract.getStatus());
        }

        @Test
        @DisplayName("getStatusEmoji retourne le bon emoji pour chaque statut")
        void statusEmojis() {
            InvestmentContract c = new InvestmentContract();

            c.setStatus(ContractStatus.DRAFT);
            assertEquals("📝", c.getStatusEmoji());

            c.setStatus(ContractStatus.INVESTOR_SIGNED);
            assertEquals("✍️", c.getStatusEmoji());

            c.setStatus(ContractStatus.FULLY_SIGNED);
            assertEquals("✅", c.getStatusEmoji());

            c.setStatus(ContractStatus.CANCELLED);
            assertEquals("❌", c.getStatusEmoji());
        }

        @Test
        @DisplayName("toString inclut le numéro de contrat et le statut")
        void toStringFormat() {
            InvestmentContract c = new InvestmentContract();
            c.setContractNumber("NAJAHNI-2025-000001");
            c.setStatus(ContractStatus.DRAFT);
            String result = c.toString();
            assertTrue(result.contains("NAJAHNI-2025-000001"));
            assertTrue(result.contains("Brouillon"));
        }

        @Test
        @DisplayName("Tous les getters/setters fonctionnent correctement")
        void gettersSetters() {
            InvestmentContract c = new InvestmentContract();
            c.setId(42);
            c.setOfferId(10);
            c.setInvestorId(5);
            c.setEntrepreneurId(7);
            c.setContractNumber("NAJAHNI-2025-000042");
            c.setTermsText("Terms here");
            c.setInvestorSignature("sig_investor_base64");
            c.setEntrepreneurSignature("sig_entrepreneur_base64");
            c.setSha256Hash("abc123");
            c.setInvestorName("John Doe");
            c.setEntrepreneurName("Jane Doe");
            c.setProjectTitle("Mon Projet");
            c.setOfferAmount("10 000 €");

            assertEquals(42, c.getId());
            assertEquals(10, c.getOfferId());
            assertEquals(5, c.getInvestorId());
            assertEquals(7, c.getEntrepreneurId());
            assertEquals("NAJAHNI-2025-000042", c.getContractNumber());
            assertEquals("Terms here", c.getTermsText());
            assertEquals("sig_investor_base64", c.getInvestorSignature());
            assertEquals("sig_entrepreneur_base64", c.getEntrepreneurSignature());
            assertEquals("abc123", c.getSha256Hash());
            assertEquals("John Doe", c.getInvestorName());
            assertEquals("Jane Doe", c.getEntrepreneurName());
            assertEquals("Mon Projet", c.getProjectTitle());
            assertEquals("10 000 €", c.getOfferAmount());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CONTRACT STATUS ENUM
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ContractStatus — Enum")
    class ContractStatusTests {

        @Test
        @DisplayName("Tous les statuts ont un displayName non vide")
        void allStatusesHaveDisplayName() {
            for (ContractStatus status : ContractStatus.values()) {
                assertNotNull(status.getDisplayName());
                assertFalse(status.getDisplayName().isBlank());
            }
        }

        @Test
        @DisplayName("L'enum contient exactement 4 valeurs")
        void exactlyFourValues() {
            assertEquals(4, ContractStatus.values().length);
        }

        @Test
        @DisplayName("DRAFT s'affiche comme 'Brouillon'")
        void draftDisplay() {
            assertEquals("Brouillon", ContractStatus.DRAFT.getDisplayName());
        }

        @Test
        @DisplayName("FULLY_SIGNED s'affiche correctement")
        void fullySignedDisplay() {
            assertEquals("Signé par les deux parties", ContractStatus.FULLY_SIGNED.getDisplayName());
        }

        @Test
        @DisplayName("valueOf fonctionne pour chaque statut")
        void valueOfWorks() {
            assertEquals(ContractStatus.DRAFT, ContractStatus.valueOf("DRAFT"));
            assertEquals(ContractStatus.INVESTOR_SIGNED, ContractStatus.valueOf("INVESTOR_SIGNED"));
            assertEquals(ContractStatus.FULLY_SIGNED, ContractStatus.valueOf("FULLY_SIGNED"));
            assertEquals(ContractStatus.CANCELLED, ContractStatus.valueOf("CANCELLED"));
        }

        @Test
        @DisplayName("toString retourne le displayName")
        void toStringReturnsDisplayName() {
            for (ContractStatus status : ContractStatus.values()) {
                assertEquals(status.getDisplayName(), status.toString());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INTEGRITY VERIFICATION (pure logic, no DB)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Vérification d'intégrité SHA-256")
    class IntegrityTests {

        @Test
        @DisplayName("Hash calculé est cohérent avec le format contract data")
        void hashConsistencyCheck() {
            // Simulate the hash computation from generateContract
            String contractNumber = "NAJAHNI-2025-000001";
            int offerId = 6;
            int investorId = 41;
            BigDecimal amount = new BigDecimal("5000");
            int oppId = 3;
            int projectId = 16;
            String terms = "CONTRAT D'INVESTISSEMENT NUMÉRIQUE";

            String hashData = contractNumber + "|" + offerId + "|" + investorId + "|"
                    + amount + "|" + oppId + "|" + projectId + "|" + terms;

            String hash = ContractService.sha256(hashData);
            assertNotNull(hash);
            assertEquals(64, hash.length());

            // Re-computing must give the same hash
            assertEquals(hash, ContractService.sha256(hashData));
        }

        @Test
        @DisplayName("Modification des données invalide le hash")
        void tamperDetection() {
            String original = "NAJAHNI-2025-000001|6|41|5000|3|16|terms";
            String tampered = "NAJAHNI-2025-000001|6|41|5001|3|16|terms"; // changed amount

            String hashOriginal = ContractService.sha256(original);
            String hashTampered = ContractService.sha256(tampered);

            assertNotEquals(hashOriginal, hashTampered,
                    "Un changement de données doit invalider le hash");
        }

        @Test
        @DisplayName("Hash change quand la signature est ajoutée")
        void hashChangesWithSignature() {
            // Simulating the verifyIntegrity data format
            String dataUnsigned = "NAJAHNI-2025-000001|6|41|DRAFT|0|0|12345";
            String dataSigned = "NAJAHNI-2025-000001|6|41|INVESTOR_SIGNED|128|0|12345";

            assertNotEquals(
                    ContractService.sha256(dataUnsigned),
                    ContractService.sha256(dataSigned)
            );
        }
    }
}
