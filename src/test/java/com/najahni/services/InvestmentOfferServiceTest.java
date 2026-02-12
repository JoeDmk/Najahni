package com.najahni.services;

import com.najahni.dao.InvestmentOfferDAO;
import com.najahni.dao.InvestmentOpportunityDAO;
import com.najahni.dao.UserDAO;
import com.najahni.models.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour InvestmentOfferService.
 *
 * Utilise Mockito pour mocker les 3 DAOs injectés :
 *  - InvestmentOfferDAO     (persistence des offres)
 *  - InvestmentOpportunityDAO (vérifier que l'opportunité est OPEN)
 *  - UserDAO                  (vérifier que l'utilisateur est INVESTOR)
 *
 * Aucune base de données ni JavaFX n'est nécessaire pour ces tests.
 *
 * @ExtendWith(MockitoExtension.class) : active l'injection des @Mock
 */
@ExtendWith(MockitoExtension.class)
class InvestmentOfferServiceTest {

    /**
     * Mock du DAO des offres d'investissement.
     * Simule les opérations CRUD sans accéder à MySQL.
     */
    @Mock
    private InvestmentOfferDAO offerDAO;

    /**
     * Mock du DAO des opportunités.
     * Utilisé par le service pour vérifier que l'opportunité est OPEN.
     */
    @Mock
    private InvestmentOpportunityDAO opportunityDAO;

    /**
     * Mock du DAO des utilisateurs.
     * Utilisé par le service pour vérifier que l'utilisateur a le rôle INVESTOR.
     */
    @Mock
    private UserDAO userDAO;

    /** Service sous test (SUT). */
    private InvestmentOfferService service;

    /**
     * @BeforeEach : exécuté avant chaque test.
     * Crée une nouvelle instance du service avec les 3 DAOs mockés
     * via le constructeur d'injection de dépendances.
     */
    @BeforeEach
    void setUp() {
        service = new InvestmentOfferService(offerDAO, opportunityDAO, userDAO);
    }

    // ─── TEST 1 : Création réussie ──────────────────────────

    /**
     * Scénario nominal : toutes les conditions sont remplies.
     *
     * Pré-conditions simulées :
     *  1. L'utilisateur (id=10) existe et a le rôle INVESTOR
     *  2. L'opportunité (id=5) existe et a le statut OPEN
     *  3. Le montant proposé est valide (> 0)
     *
     * Résultat attendu : l'offre est créée avec succès.
     */
    @Test
    @DisplayName("testCreateOffer - Création réussie avec données valides")
    void testCreateOffer() {
        // ── ARRANGE ──
        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("15000.00"),     // montant proposé : 15 000 €
                OfferStatus.PENDING,             // statut initial : EN ATTENTE
                10,                              // investorId = 10
                5                                // opportunityId = 5
        );

        // Mock : l'utilisateur 10 est un INVESTOR
        User investor = new User(10, "Ahmed Ben Ali", "ahmed@email.com", "pass", Role.INVESTOR);
        when(userDAO.findById(10)).thenReturn(Optional.of(investor));

        // Mock : l'opportunité 5 est OPEN (ouverte aux offres)
        InvestmentOpportunity openOpp = new InvestmentOpportunity(
                5, new BigDecimal("100000.00"), "Projet solaire",
                LocalDate.now().plusMonths(6), OpportunityStatus.OPEN, 1
        );
        when(opportunityDAO.findById(5)).thenReturn(Optional.of(openOpp));

        // Mock : le DAO retourne l'offre créée avec id = 1
        InvestmentOffer savedOffer = new InvestmentOffer(
                1,                               // id généré par la BDD
                new BigDecimal("15000.00"),
                OfferStatus.PENDING,
                10,
                5
        );
        when(offerDAO.create(any(InvestmentOffer.class))).thenReturn(savedOffer);

        // ── ACT ──
        InvestmentOffer result = service.createOffer(offer);

        // ── ASSERT ──
        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(1, result.getId(), "L'id doit être celui retourné par le DAO");
        assertEquals(new BigDecimal("15000.00"), result.getProposedAmount());
        assertEquals(OfferStatus.PENDING, result.getStatus());
        assertEquals(10, result.getInvestorId());
        assertEquals(5, result.getOpportunityId());

        // Vérifications des appels aux mocks
        verify(userDAO, times(1)).findById(10);        // rôle vérifié
        verify(opportunityDAO, times(1)).findById(5);  // statut OPEN vérifié
        verify(offerDAO, times(1)).create(any());       // offre créée
    }

    // ─── TEST 2 : Offre sur opportunité fermée → Exception ─

    /**
     * Règle métier : on ne peut faire une offre que sur une opportunité OPEN.
     * Si l'opportunité est CLOSED ou FUNDED, une exception est levée.
     *
     * Ce test simule une opportunité avec statut CLOSED.
     */
    @Test
    @DisplayName("testOfferOnClosedOpportunityThrowsException - Offre sur opportunité fermée")
    void testOfferOnClosedOpportunityThrowsException() {
        // ── ARRANGE ──
        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("5000.00"),
                OfferStatus.PENDING,
                10,                              // investorId
                5                                // opportunityId
        );

        // Mock : l'utilisateur est un INVESTOR valide
        User investor = new User(10, "Fatma", "fatma@email.com", "pass", Role.INVESTOR);
        when(userDAO.findById(10)).thenReturn(Optional.of(investor));

        // Mock : l'opportunité 5 est CLOSED (fermée aux offres)
        InvestmentOpportunity closedOpp = new InvestmentOpportunity(
                5, new BigDecimal("100000.00"), "Projet terminé",
                LocalDate.now().plusMonths(1), OpportunityStatus.CLOSED, 1
        );
        when(opportunityDAO.findById(5)).thenReturn(Optional.of(closedOpp));

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Une offre sur une opportunité CLOSED doit lever une exception"
        );

        // Vérifier le message d'erreur
        assertTrue(
                exception.getMessage().contains("n'est pas ouverte"),
                "Le message doit indiquer que l'opportunité n'est pas ouverte"
        );

        // Le DAO des offres ne doit PAS avoir été appelé
        verify(offerDAO, never()).create(any());
    }

    // ─── TEST 3 : Montant invalide → Exception ─────────────

    /**
     * Règle métier : le montant proposé doit être > 0.
     * On teste avec un montant négatif.
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
                exception.getMessage().contains("supérieur à zéro"),
                "Le message doit indiquer que le montant doit être supérieur à zéro"
        );

        // Aucun DAO ne doit avoir été appelé (la validation échoue avant)
        verify(offerDAO, never()).create(any());
        verify(userDAO, never()).findById(anyInt());
        verify(opportunityDAO, never()).findById(anyInt());
    }

    // ─── TEST 4 : Non-INVESTOR → Exception ─────────────────

    /**
     * Règle métier : seul un utilisateur avec le rôle INVESTOR
     * peut créer une offre d'investissement.
     *
     * Ce test simule un ENTREPRENEUR qui essaie de créer une offre → refusé.
     */
    @Test
    @DisplayName("testOnlyInvestorCanCreateOffer - ENTREPRENEUR ne peut pas créer une offre")
    void testOnlyInvestorCanCreateOffer() {
        // ── ARRANGE ──
        InvestmentOffer offer = new InvestmentOffer(
                new BigDecimal("10000.00"),
                OfferStatus.PENDING,
                20,                              // investorId = 20 (mais c'est un ENTREPRENEUR)
                5
        );

        // Mock : l'utilisateur 20 est un ENTREPRENEUR (pas un INVESTOR)
        User entrepreneur = new User(20, "Sami", "sami@email.com", "pass", Role.ENTREPRENEUR);
        when(userDAO.findById(20)).thenReturn(Optional.of(entrepreneur));

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOffer(offer),
                "Un ENTREPRENEUR ne peut pas créer une offre"
        );

        assertTrue(
                exception.getMessage().contains("INVESTOR"),
                "Le message doit mentionner qu'il faut être INVESTOR"
        );

        // Le DAO des offres ne doit PAS avoir été appelé
        verify(offerDAO, never()).create(any());
    }

    // ─── TEST 5 : Montant zéro → Exception ─────────────────

    /**
     * Cas limite : le montant vaut exactement 0.
     * La règle dit « strictement supérieur à zéro ».
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

        verify(offerDAO, never()).create(any());
    }

    // ─── TEST 6 : Acceptation d'une offre ───────────────────

    /**
     * Test de la méthode acceptOffer().
     * Vérifie que le statut passe bien à ACCEPTED.
     */
    @Test
    @DisplayName("testAcceptOffer - Acceptation passe le statut à ACCEPTED")
    void testAcceptOffer() {
        // ── ARRANGE ──
        InvestmentOffer existingOffer = new InvestmentOffer(
                1, new BigDecimal("15000.00"), OfferStatus.PENDING, 10, 5
        );
        when(offerDAO.findById(1)).thenReturn(Optional.of(existingOffer));
        when(offerDAO.update(any(InvestmentOffer.class))).thenReturn(true);

        // ── ACT ──
        boolean result = service.acceptOffer(1);

        // ── ASSERT ──
        assertTrue(result, "acceptOffer doit retourner true");
        assertEquals(OfferStatus.ACCEPTED, existingOffer.getStatus(),
                "Le statut doit être passé à ACCEPTED");
        verify(offerDAO, times(1)).update(existingOffer);
    }

    // ─── TEST 7 : Rejet d'une offre ─────────────────────────

    /**
     * Test de la méthode rejectOffer().
     * Vérifie que le statut passe bien à REJECTED.
     */
    @Test
    @DisplayName("testRejectOffer - Rejet passe le statut à REJECTED")
    void testRejectOffer() {
        // ── ARRANGE ──
        InvestmentOffer existingOffer = new InvestmentOffer(
                2, new BigDecimal("8000.00"), OfferStatus.PENDING, 15, 5
        );
        when(offerDAO.findById(2)).thenReturn(Optional.of(existingOffer));
        when(offerDAO.update(any(InvestmentOffer.class))).thenReturn(true);

        // ── ACT ──
        boolean result = service.rejectOffer(2);

        // ── ASSERT ──
        assertTrue(result, "rejectOffer doit retourner true");
        assertEquals(OfferStatus.REJECTED, existingOffer.getStatus(),
                "Le statut doit être passé à REJECTED");
        verify(offerDAO, times(1)).update(existingOffer);
    }
}
