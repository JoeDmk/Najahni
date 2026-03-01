package com.najahni.services;

import com.najahni.models.*;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service de gestion des contrats d'investissement numériques.
 *
 * Fonctionnalités :
 * - Auto-création de la table investment_contract (migration)
 * - Génération automatique de contrat après paiement
 * - Signature numérique (investisseur + entrepreneur)
 * - Vérification d'intégrité SHA-256
 * - Génération de numéro de contrat unique
 */
public class ContractService {

    private static final Logger LOG = Logger.getLogger(ContractService.class.getName());
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final Connection cnx;

    public ContractService() {
        this.cnx = DBConnection.getInstance().getConnection();
        ensureContractTable();
    }

    // ─── AUTO-MIGRATION ──────────────────────────────────────

    private void ensureContractTable() {
        try {
            DatabaseMetaData meta = cnx.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "investment_contract", null)) {
                if (!rs.next()) {
                    createContractTable();
                } else {
                    // Table exists — drop old FK constraints that may cause insert failures
                    dropForeignKeyIfExists("fk_contract_offer");
                    dropForeignKeyIfExists("fk_contract_investor");
                    dropForeignKeyIfExists("fk_contract_entrepreneur");
                    // Repair contracts with entrepreneur_id = 0
                    repairEntrepreneurIds();
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠ Could not verify contract table: " + e.getMessage());
            LOG.warning("⚠ Could not verify contract table: " + e.getMessage());
        }
    }

    private void createContractTable() {
        try (Statement stmt = cnx.createStatement()) {
            // No FK constraints — avoids silent failures with missing references
            stmt.executeUpdate("""
                CREATE TABLE investment_contract (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    offer_id INT NOT NULL,
                    investor_id INT NOT NULL,
                    entrepreneur_id INT NOT NULL DEFAULT 0,
                    contract_number VARCHAR(50) NOT NULL UNIQUE,
                    status ENUM('DRAFT','INVESTOR_SIGNED','FULLY_SIGNED','CANCELLED') DEFAULT 'DRAFT',
                    terms_text TEXT,
                    investor_signature LONGTEXT,
                    entrepreneur_signature LONGTEXT,
                    sha256_hash VARCHAR(64),
                    investor_signed_at TIMESTAMP NULL,
                    entrepreneur_signed_at TIMESTAMP NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_contract_offer (offer_id),
                    INDEX idx_contract_investor (investor_id),
                    INDEX idx_contract_entrepreneur (entrepreneur_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✓ Migration: created investment_contract table");
            LOG.info("✓ Migration: created investment_contract table");
        } catch (SQLException e) {
            System.err.println("⚠ Could not create contract table: " + e.getMessage());
            LOG.warning("⚠ Could not create contract table: " + e.getMessage());
        }
    }

    /**
     * Repairs contracts where entrepreneur_id was left as 0.
     * Looks up the correct entrepreneur via offer → opportunity → project chain.
     * Handles both cases: projet with or without entrepreneur_id column.
     */
    private void repairEntrepreneurIds() {
        // First try the direct JOIN approach (if projet has entrepreneur_id)
        try (Statement stmt = cnx.createStatement()) {
            int updated = stmt.executeUpdate("""
                UPDATE investment_contract c
                JOIN investment_offer io ON c.offer_id = io.id
                JOIN investment_opportunity iop ON io.opportunity_id = iop.id
                JOIN projet p ON iop.project_id = p.id
                SET c.entrepreneur_id = p.entrepreneur_id
                WHERE c.entrepreneur_id = 0 AND p.entrepreneur_id IS NOT NULL AND p.entrepreneur_id > 0
                """);
            if (updated > 0) {
                System.out.println("✓ Repaired entrepreneur_id on " + updated + " contract(s)");
                LOG.info("✓ Repaired entrepreneur_id on " + updated + " contract(s)");
            }
        } catch (SQLException e) {
            // projet.entrepreneur_id column might not exist yet — that's OK
            System.out.println("[ContractService] repair via projet.entrepreneur_id not available: " + e.getMessage());
        }
    }

    private void dropForeignKeyIfExists(String fkName) {
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate("ALTER TABLE investment_contract DROP FOREIGN KEY " + fkName);
            System.out.println("✓ Dropped old FK constraint: " + fkName);
        } catch (SQLException ignored) {
            // FK doesn't exist — that's fine
        }
    }

    // ─── CONTRACT GENERATION ─────────────────────────────────

    /**
     * Generates a contract automatically after a paid investment.
     * Returns the created contract, or existing one if already exists.
     */
    public InvestmentContract generateContract(InvestmentOffer offer,
                                                InvestmentOpportunity opportunity,
                                                Project project) {
        // Check if contract already exists for this offer
        Optional<InvestmentContract> existing = findByOfferId(offer.getId());
        if (existing.isPresent()) return existing.get();

        InvestmentContract contract = new InvestmentContract();
        contract.setOfferId(offer.getId());
        contract.setInvestorId(offer.getInvestorId());

        // Resolve entrepreneur ID — try project first, then look up from opportunity
        int entrepreneurId = 0;
        if (project != null && project.getEntrepreneurId() > 0) {
            entrepreneurId = project.getEntrepreneurId();
        }
        contract.setEntrepreneurId(entrepreneurId);
        System.out.println("[ContractService] Generating contract for offer #" + offer.getId()
                + " investor=" + offer.getInvestorId() + " entrepreneur=" + entrepreneurId);
        contract.setContractNumber(generateContractNumber());
        contract.setStatus(ContractStatus.DRAFT);

        // Generate legal terms text
        String terms = generateTermsText(offer, opportunity, project);
        contract.setTermsText(terms);

        // Compute SHA-256 hash of all contract data
        String hashData = contract.getContractNumber() + "|"
                + offer.getId() + "|"
                + offer.getInvestorId() + "|"
                + offer.getProposedAmount() + "|"
                + (opportunity != null ? opportunity.getId() : 0) + "|"
                + (project != null ? project.getId() : 0) + "|"
                + terms;
        contract.setSha256Hash(sha256(hashData));

        // Persist
        String sql = """
            INSERT INTO investment_contract
            (offer_id, investor_id, entrepreneur_id, contract_number, status, terms_text, sha256_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, contract.getOfferId());
            ps.setInt(2, contract.getInvestorId());
            ps.setInt(3, contract.getEntrepreneurId());
            ps.setString(4, contract.getContractNumber());
            ps.setString(5, contract.getStatus().name());
            ps.setString(6, contract.getTermsText());
            ps.setString(7, contract.getSha256Hash());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) contract.setId(rs.getInt(1));
            }

            System.out.println("✓ Contract generated: " + contract.getContractNumber());
            LOG.info("✓ Contract generated: " + contract.getContractNumber());
            return contract;
        } catch (SQLException e) {
            System.err.println("✗ Error creating contract: " + e.getMessage());
            LOG.severe("✗ Error creating contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─── SIGNATURE ───────────────────────────────────────────

    /**
     * Investor signs the contract (stores Base64 signature image).
     */
    public boolean signByInvestor(int contractId, String signatureBase64) {
        String sql = """
            UPDATE investment_contract
            SET investor_signature = ?, status = 'INVESTOR_SIGNED', investor_signed_at = NOW()
            WHERE id = ? AND status = 'DRAFT'
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, signatureBase64);
            ps.setInt(2, contractId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                LOG.info("✓ Contract " + contractId + " signed by investor");
                updateHash(contractId);
            }
            return ok;
        } catch (SQLException e) {
            LOG.severe("✗ Error signing contract (investor): " + e.getMessage());
            return false;
        }
    }

    /**
     * Entrepreneur signs the contract → becomes FULLY_SIGNED.
     */
    public boolean signByEntrepreneur(int contractId, String signatureBase64) {
        String sql = """
            UPDATE investment_contract
            SET entrepreneur_signature = ?, status = 'FULLY_SIGNED', entrepreneur_signed_at = NOW()
            WHERE id = ? AND status = 'INVESTOR_SIGNED'
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, signatureBase64);
            ps.setInt(2, contractId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                LOG.info("✓ Contract " + contractId + " fully signed");
                updateHash(contractId);
            }
            return ok;
        } catch (SQLException e) {
            LOG.severe("✗ Error signing contract (entrepreneur): " + e.getMessage());
            return false;
        }
    }

    // ─── INTEGRITY VERIFICATION ──────────────────────────────

    /**
     * Verifies the SHA-256 integrity of a contract.
     * Returns true if the stored hash matches a freshly computed hash.
     */
    public boolean verifyIntegrity(InvestmentContract contract) {
        String data = contract.getContractNumber() + "|"
                + contract.getOfferId() + "|"
                + contract.getInvestorId() + "|"
                + contract.getStatus().name() + "|"
                + (contract.getInvestorSignature() != null ? contract.getInvestorSignature().length() : 0) + "|"
                + (contract.getEntrepreneurSignature() != null ? contract.getEntrepreneurSignature().length() : 0) + "|"
                + (contract.getTermsText() != null ? contract.getTermsText().hashCode() : 0);
        String freshHash = sha256(data);
        return freshHash.equals(contract.getSha256Hash());
    }

    private void updateHash(int contractId) {
        findById(contractId).ifPresent(c -> {
            String data = c.getContractNumber() + "|"
                    + c.getOfferId() + "|"
                    + c.getInvestorId() + "|"
                    + c.getStatus().name() + "|"
                    + (c.getInvestorSignature() != null ? c.getInvestorSignature().length() : 0) + "|"
                    + (c.getEntrepreneurSignature() != null ? c.getEntrepreneurSignature().length() : 0) + "|"
                    + (c.getTermsText() != null ? c.getTermsText().hashCode() : 0);
            String newHash = sha256(data);
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE investment_contract SET sha256_hash = ? WHERE id = ?")) {
                ps.setString(1, newHash);
                ps.setInt(2, contractId);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOG.warning("Could not update hash: " + e.getMessage());
            }
        });
    }

    // ─── CRUD ────────────────────────────────────────────────

    public Optional<InvestmentContract> findById(int id) {
        String sql = """
            SELECT c.*,
                   CONCAT(u1.firstname, ' ', u1.lastname) AS investor_name,
                   CONCAT(u2.firstname, ' ', u2.lastname) AS entrepreneur_name
            FROM investment_contract c
            LEFT JOIN user u1 ON c.investor_id = u1.id
            LEFT JOIN user u2 ON c.entrepreneur_id = u2.id
            WHERE c.id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error finding contract: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<InvestmentContract> findByOfferId(int offerId) {
        String sql = """
            SELECT c.*,
                   CONCAT(u1.firstname, ' ', u1.lastname) AS investor_name,
                   CONCAT(u2.firstname, ' ', u2.lastname) AS entrepreneur_name
            FROM investment_contract c
            LEFT JOIN user u1 ON c.investor_id = u1.id
            LEFT JOIN user u2 ON c.entrepreneur_id = u2.id
            WHERE c.offer_id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error finding contract by offer: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<InvestmentContract> findByUser(int userId) {
        List<InvestmentContract> list = new ArrayList<>();
        String sql = """
            SELECT c.*,
                   CONCAT(u1.firstname, ' ', u1.lastname) AS investor_name,
                   CONCAT(u2.firstname, ' ', u2.lastname) AS entrepreneur_name
            FROM investment_contract c
            LEFT JOIN user u1 ON c.investor_id = u1.id
            LEFT JOIN user u2 ON c.entrepreneur_id = u2.id
            WHERE c.investor_id = ? OR c.entrepreneur_id = ?
            ORDER BY c.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error finding contracts by user: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds contracts linked to specific project IDs via the offer→opportunity→project chain.
     * Used for entrepreneurs to see contracts on their projects.
     */
    public List<InvestmentContract> findByProjectIds(List<Integer> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return new ArrayList<>();
        String placeholders = projectIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = """
            SELECT DISTINCT c.*,
                   CONCAT(u1.firstname, ' ', u1.lastname) AS investor_name,
                   CONCAT(u2.firstname, ' ', u2.lastname) AS entrepreneur_name
            FROM investment_contract c
            LEFT JOIN user u1 ON c.investor_id = u1.id
            LEFT JOIN user u2 ON c.entrepreneur_id = u2.id
            JOIN investment_offer io ON c.offer_id = io.id
            JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            WHERE iop.project_id IN (%s)
            ORDER BY c.created_at DESC
            """.formatted(placeholders);
        List<InvestmentContract> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < projectIds.size(); i++) {
                ps.setInt(i + 1, projectIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding contracts by project IDs: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates the entrepreneur_id on a contract.
     */
    public void updateEntrepreneurId(int contractId, int entrepreneurId) {
        String sql = "UPDATE investment_contract SET entrepreneur_id = ? WHERE id = ? AND (entrepreneur_id = 0 OR entrepreneur_id IS NULL)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            ps.setInt(2, contractId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✓ Updated entrepreneur_id=" + entrepreneurId + " on contract #" + contractId);
            }
        } catch (SQLException e) {
            System.err.println("✗ Error updating entrepreneur_id: " + e.getMessage());
        }
    }

    public List<InvestmentContract> findAll() {
        List<InvestmentContract> list = new ArrayList<>();
        String sql = """
            SELECT c.*,
                   CONCAT(u1.firstname, ' ', u1.lastname) AS investor_name,
                   CONCAT(u2.firstname, ' ', u2.lastname) AS entrepreneur_name
            FROM investment_contract c
            LEFT JOIN user u1 ON c.investor_id = u1.id
            LEFT JOIN user u2 ON c.entrepreneur_id = u2.id
            ORDER BY c.created_at DESC
            """;
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            LOG.severe("Error fetching contracts: " + e.getMessage());
        }
        return list;
    }

    // ─── HELPERS ─────────────────────────────────────────────

    private String generateContractNumber() {
        int year = java.time.Year.now().getValue();
        String sql = "SELECT COUNT(*) + 1 FROM investment_contract WHERE YEAR(created_at) = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return String.format("NAJAHNI-%d-%06d", year, rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error generating contract number: " + e.getMessage());
        }
        return "NAJAHNI-" + year + "-" + System.currentTimeMillis() % 100000;
    }

    private String generateTermsText(InvestmentOffer offer,
                                      InvestmentOpportunity opportunity,
                                      Project project) {
        String projectName = project != null && project.getTitle() != null ? project.getTitle() : "Non spécifié";
        String sector = project != null && project.getSector() != null ? project.getSector() : "Non spécifié";
        String amount = offer.getFormattedAmount();
        String target = opportunity != null ? opportunity.getFormattedAmount() : "N/A";
        String deadline = opportunity != null && opportunity.getDeadline() != null
                ? opportunity.getDeadline().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "Non définie";

        return """
                CONTRAT D'INVESTISSEMENT NUMÉRIQUE
                ═══════════════════════════════════
                
                ENTRE LES PARTIES :
                
                1. L'INVESTISSEUR (ci-après « l'Investisseur »)
                   Identifié par son compte utilisateur sur la plateforme NAJAHNI.
                
                2. L'ENTREPRENEUR (ci-après « l'Entrepreneur »)
                   Propriétaire du projet « %s » dans le secteur « %s ».
                
                ─── ARTICLE 1 : OBJET DU CONTRAT ───
                Le présent contrat régit les conditions de l'investissement réalisé
                par l'Investisseur au profit du projet de l'Entrepreneur via la
                plateforme NAJAHNI.
                
                ─── ARTICLE 2 : MONTANT DE L'INVESTISSEMENT ───
                L'Investisseur s'engage à verser la somme de %s.
                Le montant cible de l'opportunité est de %s.
                La date limite de l'opportunité est fixée au %s.
                
                ─── ARTICLE 3 : OBLIGATIONS DE L'ENTREPRENEUR ───
                L'Entrepreneur s'engage à :
                - Utiliser les fonds conformément à la description du projet
                - Fournir des rapports d'avancement réguliers
                - Informer l'Investisseur de tout changement significatif
                
                ─── ARTICLE 4 : DROITS DE L'INVESTISSEUR ───
                L'Investisseur bénéficie de :
                - Un droit d'information sur l'utilisation des fonds
                - Un reçu de paiement sécurisé via Stripe
                - L'accès au suivi du projet via la plateforme
                
                ─── ARTICLE 5 : CONFIDENTIALITÉ ───
                Les parties s'engagent à maintenir la confidentialité des
                informations échangées dans le cadre de cet investissement.
                
                ─── ARTICLE 6 : INTÉGRITÉ NUMÉRIQUE ───
                Ce contrat est protégé par un hash SHA-256 garantissant son
                intégrité. Toute modification sera détectée automatiquement.
                
                ─── ARTICLE 7 : SIGNATURES ───
                Ce contrat entre en vigueur dès sa signature par les deux parties
                via la plateforme NAJAHNI.
                
                Fait en format numérique sur la plateforme NAJAHNI.
                """.formatted(projectName, sector, amount, target, deadline);
    }

    private InvestmentContract mapResultSet(ResultSet rs) throws SQLException {
        InvestmentContract c = new InvestmentContract();
        c.setId(rs.getInt("id"));
        c.setOfferId(rs.getInt("offer_id"));
        c.setInvestorId(rs.getInt("investor_id"));
        c.setEntrepreneurId(rs.getInt("entrepreneur_id"));
        c.setContractNumber(rs.getString("contract_number"));
        c.setStatus(ContractStatus.valueOf(rs.getString("status")));
        c.setTermsText(rs.getString("terms_text"));
        c.setInvestorSignature(rs.getString("investor_signature"));
        c.setEntrepreneurSignature(rs.getString("entrepreneur_signature"));
        c.setSha256Hash(rs.getString("sha256_hash"));

        Timestamp investorSigned = rs.getTimestamp("investor_signed_at");
        if (investorSigned != null) c.setInvestorSignedAt(investorSigned.toLocalDateTime());
        Timestamp entrepreneurSigned = rs.getTimestamp("entrepreneur_signed_at");
        if (entrepreneurSigned != null) c.setEntrepreneurSignedAt(entrepreneurSigned.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());

        try { c.setInvestorName(rs.getString("investor_name")); } catch (SQLException ignored) {}
        try { c.setEntrepreneurName(rs.getString("entrepreneur_name")); } catch (SQLException ignored) {}

        return c;
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "error-" + input.hashCode();
        }
    }
}
