-- ============================================
-- NAJAHNI - Module Investissement
-- Script SQL complet (2 tables séparées)
-- ============================================

USE najahni_db;

-- ============================================
-- Suppression des anciennes tables
-- ============================================
DROP TABLE IF EXISTS investment_offer;
DROP TABLE IF EXISTS investment_opportunity;

-- ============================================
-- Table: investment_opportunity
-- Représente une demande de financement créée
-- par un ENTREPRENEUR pour un projet donné.
-- ============================================
CREATE TABLE investment_opportunity (
    id INT PRIMARY KEY AUTO_INCREMENT,
    target_amount DECIMAL(15, 2) NOT NULL,
    description TEXT,
    deadline DATE,
    status ENUM('OPEN', 'CLOSED', 'FUNDED') DEFAULT 'OPEN',
    project_id INT NOT NULL,
    risk_score DOUBLE DEFAULT NULL COMMENT 'Score de risque IA (0-100)',
    risk_label VARCHAR(20) DEFAULT NULL COMMENT 'Label ML: faible, moyen, eleve',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_opportunity_project
        FOREIGN KEY (project_id) REFERENCES projet(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: investment_offer
-- Représente une offre de financement proposée
-- par un INVESTOR en réponse à une opportunité.
-- ============================================
CREATE TABLE investment_offer (
    id INT PRIMARY KEY AUTO_INCREMENT,
    proposed_amount DECIMAL(15, 2) NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    investor_id INT NOT NULL,
    opportunity_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_offer_investor
        FOREIGN KEY (investor_id) REFERENCES user(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_offer_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES investment_opportunity(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Index pour les performances
-- ============================================
CREATE INDEX idx_opportunity_project ON investment_opportunity(project_id);
CREATE INDEX idx_opportunity_status ON investment_opportunity(status);
CREATE INDEX idx_opportunity_deadline ON investment_opportunity(deadline);

CREATE INDEX idx_offer_investor ON investment_offer(investor_id);
CREATE INDEX idx_offer_opportunity ON investment_offer(opportunity_id);
CREATE INDEX idx_offer_status ON investment_offer(status);

-- ============================================
-- Données d'exemple
-- ============================================

-- ============================================
-- MIGRATION : Ajout du risk_score (exécuter si la table existe déjà)
-- ALTER TABLE investment_opportunity ADD COLUMN risk_score DOUBLE DEFAULT NULL COMMENT 'Score de risque IA (0-100)';
-- ALTER TABLE investment_opportunity ADD COLUMN risk_label VARCHAR(20) DEFAULT NULL COMMENT 'Label ML: faible, moyen, eleve';
-- ============================================

INSERT INTO investment_opportunity (target_amount, description, deadline, status, project_id) VALUES
(100000.00, 'Financement initial pour le lancement de la plateforme technologique', '2026-06-30', 'OPEN', 1),
(50000.00, 'Recherche et développement en énergie renouvelable', '2026-09-15', 'OPEN', 2),
(75000.00, 'Développement de contenu éducatif en ligne', '2026-12-01', 'OPEN', 3);

INSERT INTO investment_offer (proposed_amount, status, investor_id, opportunity_id) VALUES
(25000.00, 'PENDING', 2, 1),
(50000.00, 'ACCEPTED', 2, 1),
(30000.00, 'PENDING', 2, 2);
