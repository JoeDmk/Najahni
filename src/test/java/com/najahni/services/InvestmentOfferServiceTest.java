package com.najahni.services;

import com.najahni.models.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour InvestmentOfferService.
 *
 * Utilise Mockito pour mocker la Connection JDBC directement.
 * Architecture testée : Service → JDBC (Connection mockée)
 *
 * Tests de validation (montant négatif, zéro) : aucun mock JDBC nécessaire,
 * car la validation échoue avant tout accès à la base de données.
 *
 * Tests métier (rôle investisseur, opportunité ouverte) : on configure
 * des PreparedStatement/ResultSet mockés pour simuler les réponses SQL.
 *
 * @ExtendWith(MockitoExtension.class) : active l'injection des @Mock
 */
@ExtendWith(MockitoExtension.class)
class InvestmentOfferServiceTest {

    /** Mock de la connexion JDBC. Simule la base de données. */
    @Mock private Connection cnx;

    /** PreparedStatement pour vérifier le rôle de l'utilisateur. */
    @Mock private PreparedStatement psCheckUser;

    /** PreparedStatement pour vérifier le statut de l'opportunité. */
    @Mock private PreparedStatement psCheckOpp;

    /** PreparedStatement pour l'insertion d'une offre. */
    @Mock private PreparedStatement psInsert;

    /** PreparedStatement pour la recherche d'une offre par ID. */
    @Mock private PreparedStatement psFindById;

    /** PreparedStatement pour la mise à jour d'une offre. */
    @Mock private PreparedStatement psUpdate;

    @Mock private ResultSet rsCheckUser;
    @Mock private ResultSet rsCheckOpp;
    @Mock private ResultSet rsKeys;
    @Mock private ResultSet rsFindById;

    /** Service sous test (SUT). */
    private InvestmentOfferService service;

    /**
     * @BeforeEach : exécuté avant chaque test.
     * Crée une nouvelle instance du service avec la Connection mockée
     * via le constructeur dédié aux tests.
     */
    @BeforeEach
    void setUp() {
        service = new InvestmentOfferService(cnx);
    }

    // ─── TEST 1 : Création réussie ──────────────────────────

    /**
     * Scénario nominal : toutes les conditions sont remplies.
     *
     * Pré-conditions simulées :
     *  1. L'utilisateur (id=10) existe et a le rôle INVESTISSEUR
     *  2. L'opportunité (id=5) existe et a le statut OPEN
     *  3. Le montant proposé est valide (> 0)
     *
     * On mocke les PreparedStatement pour les requêtes SQL :
     *  - SELECT role FROM user → INVESTISSEUR
     *  - SELECT status FROM investment_opportunity → OPEN
     *  - INSERT INTO investment_offer → retourne l'id généré = 1
     */
    @Test
    @DisplayName("testCreateOffer - Création réussie avec données valides")
    void testCreateOffer() throws SQLException {
        // ── ARRANGE ──
        // Mock des SELECT de validation (1-arg prepareStatement)
        when(cnx.prepareStatement(anyString())).thenReturn(psCheckUser, psCheckOpp);

        // Mock : l'utilisateur 10 est un INVESTISSEUR
        when(psCheckUser.executeQuery()).thenReturn(rsCheckUser);
        when(rsCheckUser.next()).thenReturn(true);
        when(rsCheckUser.getString("role")).thenReturn("INVESTISSEUR");

        // Mock : l'opportunité 5 est OPEN
        when(psCheckOpp.executeQuery()).thenReturn(rsCheckOpp);
        when(rsCheckOpp.next()).thenReturn(true);
        when(rsCheckOpp.getString("status")).thenReturn("OPEN");

        // Mock de l'INSERT (2-arg prepareStatement avec RETURN_GENERATED_KEYS)
        when(cnx.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(psInsert);
        when(psInsert.executeUpdate()).thenReturn(1);
        when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getInt(1)).thenReturn(1);

        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("15000.00"),     // montant proposé : 15 000 €
                OfferStatus.PENDING,             // statut initial : EN ATTENTE
                10,                              // investorId = 10
                5                                // opportunityId = 5
        );

        // ── ACT ──
        InvestmentOffer result = service.createOffer(offer);

        // ── ASSERT ──
        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(1, result.getId(), "L'id doit être celui retourné par la BDD");
        assertEquals(new BigDecimal("15000.00"), result.getProposedAmount());
        assertEquals(OfferStatus.PENDING, result.getStatus());
        assertEquals(10, result.getInvestorId());
        assertEquals(5, result.getOpportunityId());

        // Vérifier que l'INSERT a bien été exécuté
        verify(psInsert).executeUpdate();
    }

    // ─── TEST 2 : Offre sur opportunité fermée → Exception ─

    /**
     * Règle métier : on ne peut faire une offre que sur une opportunité OPEN.
     * Si l'opportunité est CLOSED, une exception est levée.
     *
     * On mocke le rôle utilisateur comme valide (INVESTISSEUR),
     * mais l'opportunité retourne le statut CLOSED.
     */
    @Test
    @DisplayName("testOfferOnClosedOpportunityThrowsException - Offre sur opportunité fermée")
    void testOfferOnClosedOpportunityThrowsException() throws SQLException {
        // ── ARRANGE ──
        when(cnx.prepareStatement(anyString())).thenReturn(psCheckUser, psCheckOpp);

        // Mock : l'utilisateur est un INVESTISSEUR valide
        when(psCheckUser.executeQuery()).thenReturn(rsCheckUser);
        when(rsCheckUser.next()).thenReturn(true);
        when(rsCheckUser.getString("role")).thenReturn("INVESTISSEUR");

        // Mock : l'opportunité 5 est CLOSED (fermée aux offres)
        when(psCheckOpp.executeQuery()).thenReturn(rsCheckOpp);
        when(rsCheckOpp.next()).thenReturn(true);
        when(rsCheckOpp.getString("status")).thenReturn("CLOSED");

        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("5000.00"), OfferStatus.PENDING, 10, 5
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Une offre sur une opportunité CLOSED doit lever une exception"
        );

        assertTrue(
                exception.getMessage().contains("not open"),
                "Le message doit indiquer que l'opportunité n'est pas ouverte"
        );
    }

    // ─── TEST 3 : Montant invalide → Exception ─────────────

    /**
     * Règle métier : le montant proposé doit être > 0.
     * On teste avec un montant négatif.
     *
     * La validation échoue AVANT tout accès JDBC,
     * donc aucun mock de PreparedStatement n'est nécessaire.
     */
    @Test
    @DisplayName("testOfferAmountValidation - Montant négatif lève une exception")
    void testOfferAmountValidation() {
        // ── ARRANGE ──
        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("-500.00"),        // montant NÉGATIF → invalide
                OfferStatus.PENDING,
                10,
                5
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Un montant négatif doit lever une IllegalArgumentException"
        );

        assertTrue(
                exception.getMessage().contains("greater than zero"),
                "Le message doit indiquer que le montant doit être supérieur à zéro"
        );
    }

    // ─── TEST 4 : Non-INVESTOR → Exception ─────────────────

    /**
     * Règle métier : seul un utilisateur avec le rôle INVESTISSEUR
     * peut créer une offre d'investissement.
     *
     * On mocke l'utilisateur comme ENTREPRENEUR → exception attendue.
     */
    @Test
    @DisplayName("testOnlyInvestorCanCreateOffer - ENTREPRENEUR ne peut pas créer une offre")
    void testOnlyInvestorCanCreateOffer() throws SQLException {
        // ── ARRANGE ──
        when(cnx.prepareStatement(anyString())).thenReturn(psCheckUser);

        // Mock : l'utilisateur 20 est un ENTREPRENEUR (pas un INVESTISSEUR)
        when(psCheckUser.executeQuery()).thenReturn(rsCheckUser);
        when(rsCheckUser.next()).thenReturn(true);
        when(rsCheckUser.getString("role")).thenReturn("ENTREPRENEUR");

        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("10000.00"), OfferStatus.PENDING, 20, 5
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Un ENTREPRENEUR ne peut pas créer une offre"
        );

        assertTrue(
                exception.getMessage().contains("not an investor"),
                "Le message doit mentionner que l'utilisateur n'est pas un investisseur"
        );
    }

    // ─── TEST 5 : Montant zéro → Exception ─────────────────

    /**
     * Cas limite : le montant vaut exactement 0.
     * La règle dit « strictement supérieur à zéro ».
     *
     * Aucun mock nécessaire (validation avant JDBC).
     */
    @Test
    @DisplayName("testOfferAmountZeroThrowsException - Montant zéro lève une exception")
    void testOfferAmountZeroThrowsException() {
        // ── ARRANGE ──
        InvestmentOffer offer = new InvestmentOffer(
                BigDecimal.ZERO,                 // montant = 0 → invalide
                OfferStatus.PENDING,
                10,
                5
        );

        // ── ACT & ASSERT ──
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Un montant égal à zéro doit lever une exception"
        );
    }

    // ─── TEST 6 : Acceptation d'une offre ───────────────────

    /**
     * Test de la méthode acceptOffer().
     *
     * On mocke findById (SELECT avec JOIN) pour retourner une offre existante,
     * puis on mocke l'UPDATE pour qu'il réussisse.
     * Vérifie que le résultat est true et que l'UPDATE a été exécuté.
     */
    @Test
    @DisplayName("testAcceptOffer - Acceptation passe le statut à ACCEPTED")
    void testAcceptOffer() throws SQLException {
        // ── ARRANGE ──
        // findById (1ère requête) puis updateOffer (2ème requête)
        when(cnx.prepareStatement(anyString())).thenReturn(psFindById, psUpdate);

        // Mock : findById retourne une offre existante
        when(psFindById.executeQuery()).thenReturn(rsFindById);
        when(rsFindById.next()).thenReturn(true);
        when(rsFindById.getInt("id")).thenReturn(1);
        when(rsFindById.getBigDecimal("proposed_amount")).thenReturn(new BigDecimal("15000.00"));
        when(rsFindById.getString("status")).thenReturn("PENDING");
        when(rsFindById.getInt("investor_id")).thenReturn(10);
        when(rsFindById.getInt("opportunity_id")).thenReturn(5);

        // Mock : UPDATE réussit (1 ligne modifiée)
        when(psUpdate.executeUpdate()).thenReturn(1);

        // ── ACT ──
        boolean result = service.acceptOffer(1);

        // ── ASSERT ──
        assertTrue(result, "acceptOffer doit retourner true");
        verify(psUpdate).executeUpdate();
    }

    // ─── TEST 7 : Rejet d'une offre ─────────────────────────

    /**
     * Test de la méthode rejectOffer().
     *
     * Même approche que testAcceptOffer :
     * on mocke findById + UPDATE, puis on vérifie le résultat.
     */
    @Test
    @DisplayName("testRejectOffer - Rejet passe le statut à REJECTED")
    void testRejectOffer() throws SQLException {
        // ── ARRANGE ──
        when(cnx.prepareStatement(anyString())).thenReturn(psFindById, psUpdate);

        // Mock : findById retourne une offre existante
        when(psFindById.executeQuery()).thenReturn(rsFindById);
        when(rsFindById.next()).thenReturn(true);
        when(rsFindById.getInt("id")).thenReturn(2);
        when(rsFindById.getBigDecimal("proposed_amount")).thenReturn(new BigDecimal("8000.00"));
        when(rsFindById.getString("status")).thenReturn("PENDING");
        when(rsFindById.getInt("investor_id")).thenReturn(15);
        when(rsFindById.getInt("opportunity_id")).thenReturn(5);

        // Mock : UPDATE réussit
        when(psUpdate.executeUpdate()).thenReturn(1);

        // ── ACT ──
        boolean result = service.rejectOffer(2);

        // ── ASSERT ──
        assertTrue(result, "rejectOffer doit retourner true");
        verify(psUpdate).executeUpdate();
    }
}
