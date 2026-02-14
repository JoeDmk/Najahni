-- =============================================
-- SCHEMA POUR LE MODULE APPRENTISSAGE & GAMIFICATION
-- Base de données: najahni_db
-- Tables: cours, progression, badge (3 tables uniquement)
-- =============================================

USE najahni_db;

-- =============================================
-- TABLE: cours
-- Gestion des cours d'apprentissage
-- =============================================
CREATE TABLE IF NOT EXISTS cours (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    categorie VARCHAR(100) NOT NULL DEFAULT 'Général',
    niveau_difficulte ENUM('DEBUTANT', 'INTERMEDIAIRE', 'AVANCE', 'EXPERT') NOT NULL DEFAULT 'DEBUTANT',
    points_xp INT NOT NULL DEFAULT 100,
    duree_estimee INT DEFAULT 60 COMMENT 'Durée en minutes',
    image_url VARCHAR(500),
    certification BOOLEAN DEFAULT FALSE,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_categorie (categorie),
    INDEX idx_niveau (niveau_difficulte),
    INDEX idx_actif (actif)
);

-- =============================================
-- TABLE: progression
-- Suivi de la progression des utilisateurs
-- =============================================
CREATE TABLE IF NOT EXISTS progression (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    cours_id INT NOT NULL,
    pourcentage DECIMAL(5,2) DEFAULT 0.00 CHECK (pourcentage >= 0 AND pourcentage <= 100),
    points_xp INT DEFAULT 0,
    niveau INT DEFAULT 1 CHECK (niveau >= 1 AND niveau <= 10),
    etat ENUM('NON_COMMENCE', 'EN_COURS', 'COMPLETE', 'CERTIFIE') DEFAULT 'NON_COMMENCE',
    date_debut TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_obtention TIMESTAMP NULL COMMENT 'Date de complétion du cours',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_cours (user_id, cours_id),
    INDEX idx_user_progression (user_id),
    INDEX idx_cours_progression (cours_id),
    INDEX idx_etat (etat)
);

-- =============================================
-- TABLE: badge
-- Système de récompenses et badges
-- =============================================
CREATE TABLE IF NOT EXISTS badge (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icone VARCHAR(50) DEFAULT '🏆',
    condition_obtention TEXT NOT NULL COMMENT 'Description de la condition pour obtenir le badge',
    points_requis INT DEFAULT 0 COMMENT 'Points XP requis pour débloquer',
    cours_requis INT DEFAULT 0 COMMENT 'Nombre de cours à compléter',
    niveau_requis INT DEFAULT 0 COMMENT 'Niveau minimum requis',
    categorie VARCHAR(50) DEFAULT 'Général',
    rarete ENUM('COMMUN', 'RARE', 'EPIQUE', 'LEGENDAIRE') DEFAULT 'COMMUN',
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rarete (rarete),
    INDEX idx_actif_badge (actif)
);

-- =============================================
-- DONNEES D'EXEMPLE
-- =============================================

-- Cours d'exemple
INSERT INTO cours (titre, description, categorie, niveau_difficulte, points_xp, duree_estimee, certification) VALUES
('Introduction à l''Entrepreneuriat', 'Découvrez les bases de la création d''entreprise et lancez votre projet.', 'Entrepreneuriat', 'DEBUTANT', 100, 120, FALSE),
('Business Plan Professionnel', 'Apprenez à rédiger un business plan convaincant pour les investisseurs.', 'Entrepreneuriat', 'INTERMEDIAIRE', 200, 180, TRUE),
('Marketing Digital Fondamentaux', 'Maîtrisez les outils du marketing en ligne pour promouvoir votre entreprise.', 'Marketing', 'DEBUTANT', 150, 150, FALSE),
('Gestion Financière pour Startups', 'Gérez efficacement les finances de votre startup.', 'Finance', 'AVANCE', 300, 240, TRUE),
('Leadership et Management', 'Développez vos compétences en leadership pour diriger une équipe.', 'Management', 'INTERMEDIAIRE', 250, 200, TRUE);

-- Badges
INSERT INTO badge (nom, description, icone, condition_obtention, points_requis, cours_requis, niveau_requis, categorie, rarete) VALUES
('Premier Pas', 'Bienvenue dans l''aventure ! Vous avez complété votre premier cours.', '🎯', 'Compléter 1 cours', 0, 1, 0, 'Progression', 'COMMUN'),
('Explorateur', 'Vous avez exploré 3 cours différents. Continuez votre apprentissage !', '🗺️', 'Compléter 3 cours', 0, 3, 0, 'Progression', 'COMMUN'),
('Étudiant Assidu', 'Vous avez atteint 500 points XP. Bravo pour votre persévérance !', '📚', 'Atteindre 500 XP', 500, 0, 0, 'XP', 'RARE'),
('Expert en Devenir', 'Vous avez atteint 1000 points XP. Vous êtes sur la bonne voie !', '⭐', 'Atteindre 1000 XP', 1000, 0, 0, 'XP', 'RARE'),
('Niveau 5', 'Vous avez atteint le niveau 5. Vous progressez rapidement !', '🏅', 'Atteindre le niveau 5', 0, 0, 5, 'Niveau', 'EPIQUE'),
('Maître Entrepreneur', 'Vous avez complété 5 cours. Vous êtes un vrai professionnel !', '🎓', 'Compléter 5 cours', 0, 5, 0, 'Progression', 'EPIQUE'),
('Légende', 'Vous avez atteint 5000 points XP. Vous êtes une légende !', '👑', 'Atteindre 5000 XP', 5000, 0, 0, 'XP', 'LEGENDAIRE'),
('Certifié', 'Vous avez obtenu votre première certification.', '📜', 'Obtenir une certification', 0, 0, 0, 'Certification', 'RARE');

-- Progressions d'exemple (pour l'utilisateur ID 1)
INSERT INTO progression (user_id, cours_id, pourcentage, points_xp, niveau, etat, date_obtention) VALUES
(1, 1, 100.00, 100, 2, 'COMPLETE', NOW() - INTERVAL 7 DAY),
(1, 2, 60.00, 80, 1, 'EN_COURS', NULL),
(1, 3, 25.00, 30, 1, 'EN_COURS', NULL);

SELECT 'Schema Apprentissage créé avec succès - 3 tables: cours, progression, badge' AS Message;
