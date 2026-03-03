package services.investissement;

import models.investissement.Contract;
import models.investissement.Contract.ContractStatus;
import tools.MyConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service for managing investment contracts.
 * Handles creation, SHA-256 signing, and retrieval of contracts.
 * Auto-creates the contract table if it doesn't exist.
 */
public class ContractService {

    private static final Logger LOG = Logger.getLogger(ContractService.class.getName());
    private static final String SALT = "NAJAHNI_CONTRACT_2025";

    private final Connection cnx;

    public ContractService() {
        this.cnx = MyConnection.getInstance().getConnection();
        ensureTable();
    }

    /**
     * Auto-creates the contract table if it doesn't exist.
     */
    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS investment_contract (
                id INT AUTO_INCREMENT PRIMARY KEY,
                offer_id INT NOT NULL,
                investor_id INT NOT NULL,
                entrepreneur_id INT NOT NULL,
                investor_signature VARCHAR(255) DEFAULT NULL,
                entrepreneur_signature VARCHAR(255) DEFAULT NULL,
                investor_signed_at TIMESTAMP NULL DEFAULT NULL,
                entrepreneur_signed_at TIMESTAMP NULL DEFAULT NULL,
                status VARCHAR(30) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_offer_contract (offer_id)
            )
            """;
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOG.warning("Could not ensure contract table: " + e.getMessage());
        }
    }

    /**
     * Creates a contract for a paid offer. 
     * Called automatically after payment or manually when viewing contracts.
     */
    public Contract createContract(int offerId, int investorId, int entrepreneurId) {
        // Check if contract already exists for this offer
        Optional<Contract> existing = findByOfferId(offerId);
        if (existing.isPresent()) return existing.get();

        String sql = "INSERT INTO investment_contract (offer_id, investor_id, entrepreneur_id, status) VALUES (?, ?, ?, 'PENDING')";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, offerId);
            ps.setInt(2, investorId);
            ps.setInt(3, entrepreneurId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Contract c = new Contract(offerId, investorId, entrepreneurId);
                    c.setId(rs.getInt(1));
                    c.setCreatedAt(LocalDateTime.now());
                    LOG.info("✅ Contract created for offer #" + offerId);
                    return c;
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error creating contract: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Signs a contract with SHA-256. 
     * The signature is: SHA-256(userId + offerId + timestamp + SALT).
     */
    public boolean signContract(int contractId, int userId, boolean isInvestor) {
        Optional<Contract> contractOpt = findById(contractId);
        if (contractOpt.isEmpty()) return false;

        Contract contract = contractOpt.get();

        // Check if user is authorized to sign
        if (isInvestor && contract.getInvestorId() != userId) return false;
        if (!isInvestor && contract.getEntrepreneurId() != userId) return false;

        // Check if already signed by this party
        if (isInvestor && contract.isInvestorSigned()) return false;
        if (!isInvestor && contract.isEntrepreneurSigned()) return false;

        // Generate SHA-256 signature
        String signature = generateSignature(userId, contract.getOfferId());
        LocalDateTime signedAt = LocalDateTime.now();

        // Determine new status
        ContractStatus newStatus;
        if (isInvestor) {
            newStatus = contract.isEntrepreneurSigned() ? ContractStatus.FULLY_SIGNED : ContractStatus.INVESTOR_SIGNED;
        } else {
            newStatus = contract.isInvestorSigned() ? ContractStatus.FULLY_SIGNED : ContractStatus.ENTREPRENEUR_SIGNED;
        }

        String column = isInvestor ? "investor_signature" : "entrepreneur_signature";
        String dateColumn = isInvestor ? "investor_signed_at" : "entrepreneur_signed_at";

        String sql = "UPDATE investment_contract SET " + column + " = ?, " + dateColumn + " = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, signature);
            ps.setTimestamp(2, Timestamp.valueOf(signedAt));
            ps.setString(3, newStatus.name());
            ps.setInt(4, contractId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                LOG.info("✅ Contract #" + contractId + " signed by " + (isInvestor ? "investor" : "entrepreneur") + " [" + signature.substring(0, 16) + "...]");
            }
            return updated;
        } catch (SQLException e) {
            LOG.warning("Error signing contract: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generate a SHA-256 digital signature.
     */
    public String generateSignature(int userId, int offerId) {
        try {
            String data = userId + ":" + offerId + ":" + System.currentTimeMillis() + ":" + SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            LOG.warning("Error generating SHA-256 signature: " + e.getMessage());
            return "error-generating-signature";
        }
    }

    /**
     * Find contract by ID.
     */
    public Optional<Contract> findById(int id) {
        String sql = "SELECT * FROM investment_contract WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Error finding contract by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find contract by offer ID.
     */
    public Optional<Contract> findByOfferId(int offerId) {
        String sql = "SELECT * FROM investment_contract WHERE offer_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Error finding contract by offer ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find all contracts for a given user (investor or entrepreneur).
     * Returns contracts with JOIN data for display.
     */
    public List<Contract> findByUser(int userId) {
        List<Contract> contracts = new ArrayList<>();
        String sql = """
            SELECT ic.*,
                   CONCAT(ui.firstname, ' ', ui.lastname) AS investor_name,
                   CONCAT(ue.firstname, ' ', ue.lastname) AS entrepreneur_name,
                   p.titre AS project_title,
                   p.secteur AS project_sector,
                   io.proposed_amount,
                   io.payment_intent_id
            FROM investment_contract ic
            LEFT JOIN user ui ON ic.investor_id = ui.id
            LEFT JOIN user ue ON ic.entrepreneur_id = ue.id
            LEFT JOIN investment_offer io ON ic.offer_id = io.id
            LEFT JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            LEFT JOIN projet p ON iop.project_id = p.id
            WHERE ic.investor_id = ? OR ic.entrepreneur_id = ?
            ORDER BY ic.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Contract c = mapResultSet(rs);
                    // Map JOIN fields
                    try { c.setInvestorName(rs.getString("investor_name")); } catch (Exception ignored) {}
                    try { c.setEntrepreneurName(rs.getString("entrepreneur_name")); } catch (Exception ignored) {}
                    try { c.setProjectTitle(rs.getString("project_title")); } catch (Exception ignored) {}
                    try { c.setProjectSector(rs.getString("project_sector")); } catch (Exception ignored) {}
                    try {
                        java.math.BigDecimal amt = rs.getBigDecimal("proposed_amount");
                        if (amt != null) {
                            String userCurrency = "EUR";
                            try {
                                var user = services.SessionService.getInstance().getCurrentUser();
                                if (user != null) userCurrency = user.getPreferredCurrency();
                            } catch (Exception ignored2) {}
                            if (userCurrency.equals("EUR")) {
                                c.setProposedAmount(String.format("%,.2f €", amt));
                            } else {
                                CurrencyService cs = new CurrencyService();
                                double converted = cs.convert(amt.doubleValue(), "EUR", userCurrency);
                                c.setProposedAmount(CurrencyService.format(converted, userCurrency));
                            }
                        } else {
                            c.setProposedAmount("N/A");
                        }
                    } catch (Exception ignored) { c.setProposedAmount("N/A"); }
                    try { c.setPaymentIntentId(rs.getString("payment_intent_id")); } catch (Exception ignored) {}
                    contracts.add(c);
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error finding contracts by user: " + e.getMessage());
            e.printStackTrace();
        }
        return contracts;
    }

    /**
     * Ensures contracts exist for all paid offers belonging to a user.
     * Creates missing contracts automatically.
     */
    public void ensureContractsForPaidOffers(int userId) {
        String sql = """
            SELECT io.id AS offer_id, io.investor_id, p.user_id AS entrepreneur_id
            FROM investment_offer io
            JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            JOIN projet p ON iop.project_id = p.id
            WHERE io.paid = 1 AND (io.investor_id = ? OR p.user_id = ?)
            AND io.id NOT IN (SELECT offer_id FROM investment_contract)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int offerId = rs.getInt("offer_id");
                    int investorId = rs.getInt("investor_id");
                    int entrepreneurId = rs.getInt("entrepreneur_id");
                    createContract(offerId, investorId, entrepreneurId);
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error ensuring contracts for paid offers: " + e.getMessage());
        }
    }

    private Contract mapResultSet(ResultSet rs) throws SQLException {
        Contract c = new Contract();
        c.setId(rs.getInt("id"));
        c.setOfferId(rs.getInt("offer_id"));
        c.setInvestorId(rs.getInt("investor_id"));
        c.setEntrepreneurId(rs.getInt("entrepreneur_id"));
        c.setInvestorSignature(rs.getString("investor_signature"));
        c.setEntrepreneurSignature(rs.getString("entrepreneur_signature"));

        Timestamp investorTs = rs.getTimestamp("investor_signed_at");
        if (investorTs != null) c.setInvestorSignedAt(investorTs.toLocalDateTime());
        Timestamp entrepreneurTs = rs.getTimestamp("entrepreneur_signed_at");
        if (entrepreneurTs != null) c.setEntrepreneurSignedAt(entrepreneurTs.toLocalDateTime());

        try {
            c.setStatus(ContractStatus.valueOf(rs.getString("status")));
        } catch (Exception e) {
            c.setStatus(ContractStatus.PENDING);
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) c.setCreatedAt(createdTs.toLocalDateTime());

        return c;
    }
}
