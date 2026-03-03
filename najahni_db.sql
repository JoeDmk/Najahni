-- ============================================================
-- Najahni Database Schema (Unified - All Modules)
-- ============================================================
-- Modules:
--   1. Gestion des Utilisateurs   (Ilyess)
--   2. Gestion des Projets        (Mahdi)
--   3. Investissement              (Youcef)
--   4. Community & Forum           (Dali)
--   5. Processus Mentorat          (Neirouz)
--   6. Apprentissage & Gamification (Isra)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `najahni_db`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `najahni_db`;

-- ============================================================
-- Drop tables in reverse FK order
-- ============================================================
DROP TABLE IF EXISTS `post_reactions`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `threads`;
DROP TABLE IF EXISTS `group_members`;
DROP TABLE IF EXISTS `group_join_request`;
DROP TABLE IF EXISTS `event_participants`;
DROP TABLE IF EXISTS `events`;
DROP TABLE IF EXISTS `posts`;
DROP TABLE IF EXISTS `groups`;
DROP TABLE IF EXISTS `badge`;
DROP TABLE IF EXISTS `progression`;
DROP TABLE IF EXISTS `cours`;
DROP TABLE IF EXISTS `investment_offer`;
DROP TABLE IF EXISTS `investment_opportunity`;
DROP TABLE IF EXISTS `mentorship_session`;
DROP TABLE IF EXISTS `mentorship_request`;
DROP TABLE IF EXISTS `mentor_availability`;
DROP TABLE IF EXISTS `donnees_business`;
DROP TABLE IF EXISTS `user_connection`;
DROP TABLE IF EXISTS `projet`;
DROP TABLE IF EXISTS `user`;


-- ============================================================
-- MODULE 1: GESTION DES UTILISATEURS (Ilyess)
-- ============================================================

CREATE TABLE `user` (
    `id`                  INT AUTO_INCREMENT PRIMARY KEY,
    `firstname`           VARCHAR(100) NOT NULL,
    `lastname`            VARCHAR(100) NOT NULL,
    `email`               VARCHAR(255) NOT NULL UNIQUE,
    `password`            VARCHAR(255) NOT NULL,
    `phone`               VARCHAR(20),
    `role`                ENUM('ADMIN', 'ENTREPRENEUR', 'MENTOR', 'INVESTISSEUR') NOT NULL DEFAULT 'ENTREPRENEUR',
    `bio`                 TEXT,
    `profile_picture`     VARCHAR(500),
    `company_name`        VARCHAR(200),
    `linkedin_url`        VARCHAR(500),
    `address`             VARCHAR(300),
    `date_of_birth`       DATE,
    `verified`            BOOLEAN NOT NULL DEFAULT FALSE,
    `phone_verified`      BOOLEAN NOT NULL DEFAULT FALSE,
    `is_active`           BOOLEAN NOT NULL DEFAULT TRUE,
    `is_banned`           BOOLEAN NOT NULL DEFAULT FALSE,
    `google_provider_id`  VARCHAR(255) DEFAULT NULL,
    `face_registered`     BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `total_xp`            INT NOT NULL DEFAULT 0 COMMENT 'Total XP for gamification (Apprentissage module)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_connection` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `follower_id` INT NOT NULL,
    `followed_id` INT NOT NULL,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_follow` (`follower_id`, `followed_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`followed_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MODULE 2: GESTION DES PROJETS (Mahdi)
-- ============================================================

CREATE TABLE `projet` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         INT DEFAULT NULL,
    `titre`           VARCHAR(255) DEFAULT NULL,
    `description`     TEXT DEFAULT NULL,
    `secteur`         VARCHAR(100) DEFAULT NULL,
    `etape`           VARCHAR(20) DEFAULT NULL,
    `statut`          VARCHAR(20) DEFAULT NULL,
    `date_creation`   DATE DEFAULT NULL,
    `statut_projet`   VARCHAR(20) DEFAULT 'BROUILLON',
    `score_global`    DOUBLE DEFAULT 0,
    `diagnostic_ia`   TEXT DEFAULT NULL,
    `date_soumission` DATE DEFAULT NULL,
    `date_evaluation` DATE DEFAULT NULL,
    `entrepreneur_id` INT DEFAULT NULL,
    KEY `idx_projet_entrepreneur` (`entrepreneur_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `donnees_business` (
    `id`                   INT AUTO_INCREMENT PRIMARY KEY,
    `taille_marche`        VARCHAR(255) DEFAULT NULL,
    `modele_revenu`        VARCHAR(255) DEFAULT NULL,
    `couts_estimes`        DOUBLE DEFAULT 0,
    `revenus_attendus`     DOUBLE DEFAULT 0,
    `niveau_risque`        VARCHAR(50) DEFAULT NULL,
    `force_equipe`         INT DEFAULT 0,
    `projet_id`            INT NOT NULL,
    `marge_estimee`        DOUBLE DEFAULT 0,
    `ratio_rentabilite`    DOUBLE DEFAULT 0,
    `score_financier`      DOUBLE DEFAULT 0,
    `score_marche`         DOUBLE DEFAULT 0,
    `score_equipe_calcule` DOUBLE DEFAULT 0,
    `score_risque_calcule` DOUBLE DEFAULT 0,
    `raw_couts`            TEXT DEFAULT NULL,
    `raw_revenus`          TEXT DEFAULT NULL,
    `raw_force_equipe`     TEXT DEFAULT NULL,
    FOREIGN KEY (`projet_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MODULE 3: INVESTISSEMENT (Youcef)
-- ============================================================

CREATE TABLE `investment_opportunity` (
    `id`            INT AUTO_INCREMENT PRIMARY KEY,
    `target_amount` DECIMAL(15,2) NOT NULL,
    `description`   TEXT DEFAULT NULL,
    `deadline`      DATE DEFAULT NULL,
    `status`        ENUM('OPEN','CLOSED','FUNDED') DEFAULT 'OPEN',
    `project_id`    INT NOT NULL,
    `risk_score`    DOUBLE DEFAULT NULL,
    `risk_label`    VARCHAR(50) DEFAULT NULL,
    `created_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_opportunity_project`  (`project_id`),
    KEY `idx_opportunity_status`   (`status`),
    KEY `idx_opportunity_deadline` (`deadline`),
    CONSTRAINT `fk_opportunity_project` FOREIGN KEY (`project_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `investment_offer` (
    `id`                INT AUTO_INCREMENT PRIMARY KEY,
    `proposed_amount`   DECIMAL(15,2) NOT NULL,
    `status`            ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
    `investor_id`       INT NOT NULL,
    `opportunity_id`    INT NOT NULL,
    `paid`              TINYINT(1) DEFAULT 0,
    `payment_intent_id` VARCHAR(255) DEFAULT NULL,
    `paid_at`           TIMESTAMP NULL DEFAULT NULL,
    `created_at`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_offer_investor`    (`investor_id`),
    KEY `idx_offer_opportunity` (`opportunity_id`),
    KEY `idx_offer_status`      (`status`),
    CONSTRAINT `fk_offer_investor`    FOREIGN KEY (`investor_id`)    REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_offer_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `investment_opportunity`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MODULE 4: COMMUNITY & FORUM (Dali)
-- ============================================================

CREATE TABLE `groups` (
    `id`             INT AUTO_INCREMENT PRIMARY KEY,
    `name`           VARCHAR(100) NOT NULL,
    `description`    TEXT DEFAULT NULL,
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `group_admin_id` INT NOT NULL,
    `is_private`     TINYINT(1) DEFAULT 0,
    KEY `idx_group_admin` (`group_admin_id`),
    CONSTRAINT `fk_groups_admin` FOREIGN KEY (`group_admin_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_members` (
    `id`        INT AUTO_INCREMENT PRIMARY KEY,
    `group_id`  INT NOT NULL,
    `user_id`   INT NOT NULL,
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_group_member` (`group_id`, `user_id`),
    KEY `idx_member_user` (`user_id`),
    CONSTRAINT `fk_gm_group` FOREIGN KEY (`group_id`) REFERENCES `groups`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_gm_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)   ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_join_request` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `group_id`   INT NOT NULL,
    `user_id`    INT NOT NULL,
    `status`     ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_gjr_group` (`group_id`),
    KEY `idx_gjr_user`  (`user_id`),
    CONSTRAINT `fk_gjr_group` FOREIGN KEY (`group_id`) REFERENCES `groups`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gjr_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `threads` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `group_id`   INT NOT NULL,
    `user_id`    INT NOT NULL,
    `title`      VARCHAR(150) NOT NULL,
    `content`    TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_thread_group` (`group_id`),
    KEY `idx_thread_user`  (`user_id`),
    CONSTRAINT `fk_thread_group` FOREIGN KEY (`group_id`) REFERENCES `groups`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_thread_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)   ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `thread_id`  INT NOT NULL,
    `user_id`    INT NOT NULL,
    `content`    TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_comment_thread` (`thread_id`),
    KEY `idx_comment_user`   (`user_id`),
    CONSTRAINT `fk_comment_thread` FOREIGN KEY (`thread_id`) REFERENCES `threads`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comment_user`   FOREIGN KEY (`user_id`)   REFERENCES `user`(`id`)    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `posts` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    INT NOT NULL,
    `content`    TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `image_url`  VARCHAR(500) DEFAULT NULL,
    KEY `idx_post_user` (`user_id`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_reactions` (
    `id`            INT AUTO_INCREMENT PRIMARY KEY,
    `post_id`       INT NOT NULL,
    `user_id`       INT NOT NULL,
    `reaction_type` ENUM('LIKE','LOVE','HAHA','WOW','SAD','ANGRY') NOT NULL DEFAULT 'LIKE',
    UNIQUE KEY `unique_reaction` (`post_id`, `user_id`),
    KEY `idx_reaction_user` (`user_id`),
    CONSTRAINT `fk_reaction_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reaction_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `events` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `title`       VARCHAR(150) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `event_date`  DATETIME NOT NULL,
    `capacity`    INT NOT NULL DEFAULT 0,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by`  INT NOT NULL,
    KEY `idx_event_creator` (`created_by`),
    CONSTRAINT `fk_event_creator` FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `event_participants` (
    `id`       INT AUTO_INCREMENT PRIMARY KEY,
    `event_id` INT NOT NULL,
    `user_id`  INT NOT NULL,
    UNIQUE KEY `unique_participation` (`event_id`, `user_id`),
    KEY `idx_ep_user` (`user_id`),
    CONSTRAINT `fk_ep_event` FOREIGN KEY (`event_id`) REFERENCES `events`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_ep_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)   ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MODULE 5: PROCESSUS MENTORAT (Neirouz)
-- ============================================================

CREATE TABLE `mentor_availability` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `mentor_id`  INT NOT NULL,
    `date`       DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time`   TIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_ma_mentor` (`mentor_id`),
    CONSTRAINT `fk_ma_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mentorship_request` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `entrepreneur_id` INT DEFAULT NULL,
    `mentor_id`       INT DEFAULT NULL,
    `project_id`      INT DEFAULT NULL,
    `date`            DATE DEFAULT NULL,
    `time`            VARCHAR(50) DEFAULT NULL,
    `motivation`      TEXT DEFAULT NULL,
    `goals`           TEXT DEFAULT NULL,
    `match_score`     FLOAT DEFAULT NULL,
    `auto_approved`   BOOLEAN DEFAULT FALSE,
    `status`          VARCHAR(50) DEFAULT 'PENDING',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_mr_entrepreneur` (`entrepreneur_id`),
    KEY `idx_mr_mentor`       (`mentor_id`),
    KEY `idx_mr_project`      (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mentorship_session` (
    `id`                     INT AUTO_INCREMENT PRIMARY KEY,
    `mentorship_request_id`  INT DEFAULT NULL,
    `scheduled_at`           DATETIME DEFAULT NULL,
    `duration_minutes`       INT DEFAULT NULL,
    `status`                 VARCHAR(50) DEFAULT 'SCHEDULED',
    `meeting_link`           VARCHAR(500) DEFAULT NULL,
    `mentor_feedback`        TEXT DEFAULT NULL,
    `entrepreneur_feedback`  TEXT DEFAULT NULL,
    `mentor_rating`          INT DEFAULT NULL,
    `entrepreneur_rating`    INT DEFAULT NULL,
    `created_at`             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`             DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_ms_request` (`mentorship_request_id`),
    CONSTRAINT `fk_ms_request` FOREIGN KEY (`mentorship_request_id`) REFERENCES `mentorship_request`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MODULE 6: APPRENTISSAGE & GAMIFICATION (Isra)
-- ============================================================

CREATE TABLE `cours` (
    `id`                 INT AUTO_INCREMENT PRIMARY KEY,
    `titre`              VARCHAR(255) NOT NULL,
    `description`        TEXT DEFAULT NULL,
    `categorie`          VARCHAR(100) NOT NULL DEFAULT 'Général',
    `niveau_difficulte`  ENUM('DEBUTANT','INTERMEDIAIRE','AVANCE','EXPERT') NOT NULL DEFAULT 'DEBUTANT',
    `points_xp`          INT NOT NULL DEFAULT 100,
    `duree_estimee`      INT DEFAULT 60 COMMENT 'Durée en minutes',
    `image_url`          VARCHAR(500) DEFAULT NULL,
    `certification`      TINYINT(1) DEFAULT 0,
    `actif`              TINYINT(1) DEFAULT 1,
    `document_path`      VARCHAR(500) DEFAULT NULL COMMENT 'Path to course document',
    `video_url`          VARCHAR(500) DEFAULT NULL COMMENT 'URL to course video',
    `created_at`         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_categorie`  (`categorie`),
    KEY `idx_niveau`     (`niveau_difficulte`),
    KEY `idx_actif_cours`(`actif`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `progression` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         INT NOT NULL,
    `cours_id`        INT NOT NULL,
    `pourcentage`     DECIMAL(5,2) DEFAULT 0.00,
    `points_xp`       INT DEFAULT 0,
    `niveau`          INT DEFAULT 1,
    `etat`            ENUM('NON_COMMENCE','EN_COURS','COMPLETE','CERTIFIE') DEFAULT 'NON_COMMENCE',
    `date_debut`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `date_obtention`  TIMESTAMP NULL DEFAULT NULL COMMENT 'Date de complétion du cours',
    `updated_at`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_user_cours` (`user_id`, `cours_id`),
    KEY `idx_user_progression`  (`user_id`),
    KEY `idx_cours_progression` (`cours_id`),
    KEY `idx_etat`              (`etat`),
    CONSTRAINT `fk_progression_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)  ON DELETE CASCADE,
    CONSTRAINT `fk_progression_cours` FOREIGN KEY (`cours_id`) REFERENCES `cours`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `badge` (
    `id`                   INT AUTO_INCREMENT PRIMARY KEY,
    `nom`                  VARCHAR(100) NOT NULL UNIQUE,
    `description`          TEXT DEFAULT NULL,
    `icone`                VARCHAR(50) DEFAULT '🏆',
    `condition_obtention`  TEXT NOT NULL COMMENT 'Description de la condition pour obtenir le badge',
    `points_requis`        INT DEFAULT 0 COMMENT 'Points XP requis pour débloquer',
    `cours_requis`         INT DEFAULT 0 COMMENT 'Nombre de cours à compléter',
    `niveau_requis`        INT DEFAULT 0 COMMENT 'Niveau minimum requis',
    `categorie`            VARCHAR(50) DEFAULT 'Général',
    `rarete`               ENUM('COMMUN','RARE','EPIQUE','LEGENDAIRE') DEFAULT 'COMMUN',
    `actif`                TINYINT(1) DEFAULT 1,
    `created_at`           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_rarete`       (`rarete`),
    KEY `idx_actif_badge`  (`actif`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comment` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `cours_id`    INT NOT NULL,
    `user_id`     INT NOT NULL,
    `contenu`     TEXT NOT NULL,
    `rating`      DECIMAL(2,1) DEFAULT NULL,
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_appr_comment_cours`  (`cours_id`),
    KEY `idx_appr_comment_user`   (`user_id`),
    CONSTRAINT `fk_appr_comment_cours` FOREIGN KEY (`cours_id`) REFERENCES `cours`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appr_comment_user`  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- Sample data for Apprentissage module
-- ============================================================
INSERT INTO `cours` (`id`, `titre`, `description`, `categorie`, `niveau_difficulte`, `points_xp`, `duree_estimee`, `certification`) VALUES
(1, 'Introduction à l''Entrepreneuriat', 'Découvrez les bases de la création d''entreprise.', 'Entrepreneuriat', 'DEBUTANT', 100, 120, 0),
(2, 'Business Plan Professionnel', 'Apprenez à rédiger un business plan convaincant.', 'Entrepreneuriat', 'INTERMEDIAIRE', 200, 180, 1),
(3, 'Marketing Digital Fondamentaux', 'Maîtrisez les outils du marketing en ligne.', 'Marketing', 'DEBUTANT', 150, 150, 0),
(4, 'Gestion Financière pour Startups', 'Gérez efficacement les finances de votre startup.', 'Finance', 'AVANCE', 300, 240, 1),
(5, 'Leadership et Management', 'Développez vos compétences en leadership.', 'Management', 'INTERMEDIAIRE', 250, 200, 1);

INSERT INTO `badge` (`id`, `nom`, `description`, `icone`, `condition_obtention`, `points_requis`, `cours_requis`, `niveau_requis`, `categorie`, `rarete`) VALUES
(1, 'Premier Pas',       'Bienvenue ! Vous avez complété votre premier cours.', '🎯', 'Compléter 1 cours',     0, 1, 0, 'Progression', 'COMMUN'),
(2, 'Explorateur',       'Vous avez exploré 3 cours différents.',                '🗺️', 'Compléter 3 cours',     0, 3, 0, 'Progression', 'COMMUN'),
(3, 'Étudiant Assidu',   'Vous avez atteint 500 points XP.',                    '📚', 'Atteindre 500 XP',    500, 0, 0, 'XP',          'RARE'),
(4, 'Expert en Devenir', 'Vous avez atteint 1000 points XP.',                   '⭐', 'Atteindre 1000 XP',  1000, 0, 0, 'XP',          'RARE'),
(5, 'Niveau 5',          'Vous avez atteint le niveau 5.',                       '🏅', 'Atteindre le niveau 5', 0, 0, 5, 'Niveau',      'EPIQUE'),
(6, 'Maître Entrepreneur','Vous avez complété 5 cours.',                         '🎓', 'Compléter 5 cours',     0, 5, 0, 'Progression', 'EPIQUE'),
(7, 'Légende',           'Vous avez atteint 5000 points XP.',                   '👑', 'Atteindre 5000 XP',  5000, 0, 0, 'XP',          'LEGENDAIRE'),
(8, 'Certifié',          'Vous avez obtenu votre première certification.',       '📜', 'Obtenir une certification', 0, 0, 0, 'Certification', 'RARE');
