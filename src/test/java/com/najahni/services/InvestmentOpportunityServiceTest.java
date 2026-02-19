package com.najahni.services;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour InvestmentOpportunityService.
 *
 * Utilise Mockito pour mocker la Connection JDBC directement.
 * Architecture testée : Service → JDBC (Connection mockée)
 *
 * Tests de validation (montant, deadline) : aucun mock JDBC nécessaire.
 * Tests métier (projet existe, fermeture) : on configure des mocks SQL.
 *
 * @ExtendWith(MockitoExtension.class) : active l'injection automatique des @Mock
 */
@ExtendWith(MockitoExtension.class)
class InvestmentOpportunityServiceTest {

    /** Mock de la connexion JDBC. */
    @Mock private Connection cnx;

    /** PreparedStatement pour vérifier l'existence du projet (SELECT COUNT). */
    @Mock private PreparedStatement psCheckProject;

    /** PreparedStatement pour l'insertion d'une opportunité. */
    @Mock private PreparedStatement psInsert;

    /** PreparedStatement pour la recherche par ID (SELECT avec JOIN). */
    @Mock private PreparedStatement psFindById;

    /** PreparedStatement pour la mise à jour. */
    @Mock private PreparedStatement psUpdate;

    @Mock private ResultSet rsCheckProject;
    @Mock private ResultSet rsKeys;
    @Mock private ResultSet rsFindById;

    /** Service sous test (SUT = System Under Test). */
    private InvestmentOpportunityService service;

    /**
     * @BeforeEach : exécuté avant CHAQUE méthode de test.
     * Crée une nouvelle instance du service avec la Connection mockée.
     */
    @BeforeEach
    void setUp() {
        service = new InvestmentOpportunityService(cnx);
    }

    // ─── TEST 1 : Création réussie ──────────────────────────

    /**
     * Scénario nominal : toutes les données sont valides.
     *
     * On mocke :
     *  - SELECT COUNT(*) FROM projet → 1 (le projet existe)
     *  - INSERT INTO investment_opportunity → retourne l'id généré = 1
     */
    @Test
    @DisplayName("testCreateOpportunity - Création réussie avec données valides")
    void testCreateOpportunity() throws SQLException {
        // ── ARRANGE ──
        // Mock du SELECT de validation (1-arg prepareStatement)
        when(cnx.prepareStatement(anyString())).thenReturn(psCheckProject);
        when(psCheckProject.executeQuery()).thenReturn(rsCheckProject);
        when(rsCheckProject.next()).thenReturn(true);
        when(rsCheckProject.getInt(1)).thenReturn(1); // Le projet existe (count = 1)

        // Mock de l'INSERT (2-arg prepareStatement)
        when(cnx.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(psInsert);
        when(psInsert.executeUpdate()).thenReturn(1);
        when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getInt(1)).thenReturn(1);

        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("50000.00"),      // montant cible : 50 000 €
                "Financement R&D IoT",           // description
                LocalDate.now().plusMonths(3),    // deadline dans 3 mois
                OpportunityStatus.OPEN,           // statut OPEN
                1                                 // projectId = 1
        );

        // ── ACT ──
        InvestmentOpportunity result = service.createOpportunity(opp);

        // ── ASSERT ──
        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(1, result.getId(), "L'id doit être celui retourné par la BDD");
        assertEquals(new BigDecimal("50000.00"), result.getTargetAmount());
        assertEquals(OpportunityStatus.OPEN, result.getStatus());

        // Vérifier que l'INSERT a bien été exécuté
        verify(psInsert).executeUpdate();
    }

    // ─── TEST 2 : Montant négatif → Exception ──────────────

    /**
     * Règle métier : le montant cible doit être > 0.
     * Si on passe un montant négatif, une IllegalArgumentException est levée.
     *
     * La validation échoue AVANT tout accès JDBC.
     */
    @Test
    @DisplayName("testAmountNegativeThrowsException - Montant négatif lève une exception")
    void testAmountNegativeThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("-1000.00"),       // montant NÉGATIF → invalide
                "Description test",
                LocalDate.now().plusDays(30),
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un montant négatif doit lever une IllegalArgumentException"
        );

        assertTrue(
                exception.getMessage().contains("greater than zero"),
                "Le message doit indiquer que le montant doit être supérieur à zéro"
        );
    }

    // ─── TEST 3 : Projet inexistant → Exception ────────────

    /**
     * Règle métier : le projet associé à l'opportunité doit exister.
     * Si SELECT COUNT(*) retourne 0, une exception est levée.
     */
    @Test
    @DisplayName("testProjectNotFoundThrowsException - Projet inexistant lève une exception")
    void testProjectNotFoundThrowsException() throws SQLException {
        // ── ARRANGE ──
        when(cnx.prepareStatement(anyString())).thenReturn(psCheckProject);
        when(psCheckProject.executeQuery()).thenReturn(rsCheckProject);
        when(rsCheckProject.next()).thenReturn(true);
        when(rsCheckProject.getInt(1)).thenReturn(0); // Le projet N'EXISTE PAS (count = 0)

        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("25000.00"),
                "Description valide",
                LocalDate.now().plusDays(60),
                OpportunityStatus.OPEN,
                999                               // projectId inexistant
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un projet inexistant doit lever une IllegalArgumentException"
        );

        assertTrue(
                exception.getMessage().contains("not found"),
                "Le message doit indiquer que le projet n'existe pas"
        );
    }

    // ─── TEST 4 : Deadline dans le passé → Exception ────────

    /**
     * Règle métier : la deadline ne peut pas être dans le passé.
     * La validation échoue AVANT tout accès JDBC.
     */
    @Test
    @DisplayName("testDeadlineInPastThrowsException - Deadline passée lève une exception")
    void testDeadlineInPastThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("10000.00"),
                "Description test",
                LocalDate.now().minusDays(10),    // deadline dans le PASSÉ
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Une deadline dans le passé doit lever une exception"
        );

        assertTrue(
                exception.getMessage().contains("past"),
                "Le message doit indiquer que la deadline ne peut pas être dans le passé"
        );
    }

    // ─── TEST 5 : Montant zéro → Exception ─────────────────

    /**
     * Cas limite : le montant vaut exactement 0.
     * La règle dit « strictement supérieur à zéro ».
     */
    @Test
    @DisplayName("testAmountZeroThrowsException - Montant égal à zéro lève une exception")
    void testAmountZeroThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                BigDecimal.ZERO,                  // montant = 0 → invalide
                "Description test",
                LocalDate.now().plusDays(30),
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un montant égal à zéro doit lever une exception"
        );
    }

    // ─── TEST 6 : Fermeture d'une opportunité ──────────────

    /**
     * Test de la méthode closeOpportunity().
     *
     * On mocke findById (SELECT avec JOIN) pour retourner une opportunité,
     * puis on mocke l'UPDATE pour qu'il réussisse.
     * Vérifie que le résultat est true et que l'UPDATE a été exécuté.
     */
    @Test
    @DisplayName("testCloseOpportunity - Fermeture passe le statut à CLOSED")
    void testCloseOpportunity() throws SQLException {
        // ── ARRANGE ──
        // findById (1ère requête) puis updateOpportunity (2ème requête)
        when(cnx.prepareStatement(anyString())).thenReturn(psFindById, psUpdate);

        // Mock : findById retourne une opportunité existante
        when(psFindById.executeQuery()).thenReturn(rsFindById);
        when(rsFindById.next()).thenReturn(true);
        when(rsFindById.getInt("id")).thenReturn(1);
        when(rsFindById.getBigDecimal("target_amount")).thenReturn(new BigDecimal("50000.00"));
        when(rsFindById.getString("description")).thenReturn("Test");
        when(rsFindById.getDate("deadline"))
                .thenReturn(java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
        when(rsFindById.getString("status")).thenReturn("OPEN");
        when(rsFindById.getInt("project_id")).thenReturn(1);

        // Mock : UPDATE réussit (1 ligne modifiée)
        when(psUpdate.executeUpdate()).thenReturn(1);

        // ── ACT ──
        boolean result = service.closeOpportunity(1);

        // ── ASSERT ──
        assertTrue(result, "closeOpportunity doit retourner true");
        verify(psUpdate).executeUpdate();
    }
}
