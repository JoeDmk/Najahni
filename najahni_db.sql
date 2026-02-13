-- ============================================
-- Najahni Database Schema
-- Module: Gestion des Utilisateurs
-- ============================================

-- Since you already created najahni_db, just run USE + the table creation:
USE najahni_db;

-- ============================================
-- Table: user
-- ============================================
DROP TABLE IF EXISTS `user_connection`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `firstname`       VARCHAR(100) NOT NULL,
    `lastname`        VARCHAR(100) NOT NULL,
    `email`           VARCHAR(255) NOT NULL UNIQUE,
    `password`        VARCHAR(255) NOT NULL,
    `phone`           VARCHAR(20),
    `role`            ENUM('ADMIN', 'ENTREPRENEUR', 'MENTOR', 'INVESTISSEUR') NOT NULL DEFAULT 'ENTREPRENEUR',
    `bio`             TEXT,
    `profile_picture` VARCHAR(500),
    `company_name`    VARCHAR(200),
    `linkedin_url`    VARCHAR(500),
    `address`         VARCHAR(300),
    `date_of_birth`   DATE,
    `verified`        BOOLEAN NOT NULL DEFAULT FALSE,
    `is_active`       BOOLEAN NOT NULL DEFAULT TRUE,
    `is_banned`       BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: user_connection (follow system)
-- ============================================
CREATE TABLE `user_connection` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `follower_id` INT NOT NULL,
    `followed_id` INT NOT NULL,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_follow` (`follower_id`, `followed_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`followed_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
