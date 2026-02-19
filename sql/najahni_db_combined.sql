-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : dim. 15 fév. 2026 à 16:26
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12
--
-- Base de données commune de l'équipe Najahni
-- Inclut les modules: User, Projet, Forum, Events, Mentorat,
--                     Investissement, Apprentissage & Gamification

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `najahni_db`
--

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `id` int(11) NOT NULL,
  `firstname` varchar(100) NOT NULL,
  `lastname` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `role` enum('ADMIN','ENTREPRENEUR','MENTOR','INVESTISSEUR') NOT NULL DEFAULT 'ENTREPRENEUR',
  `bio` text DEFAULT NULL,
  `profile_picture` varchar(500) DEFAULT NULL,
  `company_name` varchar(200) DEFAULT NULL,
  `linkedin_url` varchar(500) DEFAULT NULL,
  `address` varchar(300) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT 0,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `is_banned` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`id`, `firstname`, `lastname`, `email`, `password`, `phone`, `role`, `bio`, `profile_picture`, `company_name`, `linkedin_url`, `address`, `date_of_birth`, `verified`, `is_active`, `is_banned`, `created_at`, `updated_at`) VALUES
(1, 'dali', 'bellagha', 'm.dalilo2016@gmail.com', '1A234123', '12345678', 'ENTREPRENEUR', 'akjdna', NULL, 'dad', NULL, NULL, NULL, 0, 1, 0, '2026-02-13 10:56:13', '2026-02-14 17:49:31'),
(2, 'ZAPATISTA', '07', 'sekifonfon@gmail.com\r\n', '', NULL, 'ENTREPRENEUR', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, 0, '2026-02-13 11:12:47', '2026-02-14 17:49:20');

-- --------------------------------------------------------

--
-- Structure de la table `projet`
--

CREATE TABLE `projet` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `secteur` varchar(100) DEFAULT NULL,
  `etape` varchar(20) DEFAULT NULL,
  `statut` varchar(20) DEFAULT NULL,
  `date_creation` date DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `projet`
--

INSERT INTO `projet` (`id`, `titre`, `description`, `secteur`, `etape`, `statut`, `date_creation`) VALUES
(16, 'DALI', 'TE', 'DD', 'DD', 'DD', '2026-02-14'),
(12, 'FF', 'FF', 'FF', 'FF', 'FF', '2026-02-14'),
(13, '1', '1', '1', '1', '1', '2026-02-14'),
(14, 'jack BOUCH', 'ff', 'ff', 'ff', 'ff', '2026-02-14'),
(17, '1', '1', '1', '1', '1', '2026-02-14'),
(18, '1', '1', 'AYWAH', 'AYWAH', 'AYWAH', '2026-02-15');

-- --------------------------------------------------------

--
-- Structure de la table `comments`
--

CREATE TABLE `comments` (
  `id` int(11) NOT NULL,
  `thread_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `comments`
--

INSERT INTO `comments` (`id`, `thread_id`, `user_id`, `content`, `created_at`) VALUES
(1, 1, 2, 'Test comment', '2026-02-14 16:41:44'),
(30, 1, 1, '**** you', '2026-02-15 01:56:16');

-- --------------------------------------------------------

--
-- Structure de la table `demande_mentorat`
--

CREATE TABLE `demande_mentorat` (
  `id` int(11) NOT NULL,
  `date_demande` datetime NOT NULL,
  `statut` varchar(50) DEFAULT NULL,
  `score_matching_ai` double DEFAULT NULL,
  `entrepreneur_id` int(11) DEFAULT NULL,
  `mentor_id` int(11) DEFAULT NULL,
  `projet_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `donnees_business`
--

CREATE TABLE `donnees_business` (
  `id` int(11) NOT NULL,
  `taille_marche` varchar(50) DEFAULT NULL,
  `modele_revenu` varchar(100) DEFAULT NULL,
  `couts_estimes` double DEFAULT NULL,
  `revenus_attendus` double DEFAULT NULL,
  `niveau_risque` varchar(50) DEFAULT NULL,
  `force_equipe` int(11) DEFAULT NULL,
  `projet_id` int(11) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `donnees_business`
--

INSERT INTO `donnees_business` (`id`, `taille_marche`, `modele_revenu`, `couts_estimes`, `revenus_attendus`, `niveau_risque`, `force_equipe`, `projet_id`) VALUES
(5, 'MOYEN', 'Abonnement + publicité', 18000, 72000, 'MOYEN', 7, 7),
(4, 'MOYEN', 'Abonnement + publicité', 18000, 72000, 'MOYEN', 7, 6),
(6, 'MOYEN', 'Abonnement + publicité', 18000, 72000, 'MOYEN', 7, 8),
(7, 'MOYEN', 'Abonnement + publicité', 18000, 72000, 'MOYEN', 7, 9),
(14, '1', '1', 1, 1, '1', 1, 16),
(13, '1', '1', 1, 1, '1', 1, 15),
(10, '1', '1', 1, 1, '1', 1, 12),
(11, '1', 'F', 1, 1, 'F', 1, 13),
(12, '1', '1', 1, 1, '1', 1, 14),
(15, '1', '1', 1, 1, '1', 1, 17),
(16, '1', '1', 1, 11, '1', 1, 18);

-- --------------------------------------------------------

--
-- Structure de la table `events`
--

CREATE TABLE `events` (
  `id` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `event_date` datetime NOT NULL,
  `capacity` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `created_by` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `events`
--

INSERT INTO `events` (`id`, `title`, `description`, `event_date`, `capacity`, `created_at`, `created_by`) VALUES
(1, 'Test Event', 'event for tests', '2026-03-01 10:00:00', 10, '2026-02-14 16:41:44', 1),
(3, 'testalo', 'aa', '2026-02-14 22:27:58', 2, '2026-02-14 22:28:08', 1),
(4, 'dada', 'aaa', '2026-02-14 17:35:50', 1, '2026-02-14 17:35:59', 1);

-- --------------------------------------------------------

--
-- Structure de la table `event_participants`
--

CREATE TABLE `event_participants` (
  `id` int(11) NOT NULL,
  `event_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `event_participants`
--

INSERT INTO `event_participants` (`id`, `event_id`, `user_id`) VALUES
(12, 1, 1),
(1, 1, 2),
(16, 3, 1),
(15, 4, 1);

-- --------------------------------------------------------

--
-- Structure de la table `groups`
--

CREATE TABLE `groups` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `group_admin_id` int(11) NOT NULL,
  `is_private` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `groups`
--

INSERT INTO `groups` (`id`, `name`, `description`, `created_at`, `group_admin_id`, `is_private`) VALUES
(1, 'TestGroup', 'group for tests', '2026-02-14 16:41:44', 1, 0);

-- --------------------------------------------------------

--
-- Structure de la table `group_join_request`
--

CREATE TABLE `group_join_request` (
  `id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `group_members`
--

CREATE TABLE `group_members` (
  `id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `group_members`
--

INSERT INTO `group_members` (`id`, `group_id`, `user_id`, `joined_at`) VALUES
(1, 1, 1, '2026-02-14 16:47:58');

-- --------------------------------------------------------

--
-- Structure de la table `posts`
--

CREATE TABLE `posts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `image_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `posts`
--

INSERT INTO `posts` (`id`, `user_id`, `content`, `created_at`, `image_url`) VALUES
(1, 1, 'Test post content', '2026-02-14 16:41:44', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `post_reactions`
--

CREATE TABLE `post_reactions` (
  `id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `reaction_type` enum('LIKE','LOVE','HAHA','WOW','SAD','ANGRY') NOT NULL DEFAULT 'LIKE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `session_mentorat`
--

CREATE TABLE `session_mentorat` (
  `id` int(11) NOT NULL,
  `date_session` datetime NOT NULL,
  `duree` int(11) DEFAULT NULL,
  `statut` varchar(50) DEFAULT NULL,
  `note` int(11) DEFAULT NULL,
  `commentaire` text DEFAULT NULL,
  `entrepreneur_id` int(11) DEFAULT NULL,
  `mentor_id` int(11) DEFAULT NULL,
  `demande_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `threads`
--

CREATE TABLE `threads` (
  `id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `threads`
--

INSERT INTO `threads` (`id`, `group_id`, `user_id`, `title`, `content`, `created_at`) VALUES
(1, 1, 1, 'TestThread', 'thread for tests', '2026-02-14 16:41:44');

-- --------------------------------------------------------

--
-- Structure de la table `user_connection`
--

CREATE TABLE `user_connection` (
  `id` int(11) NOT NULL,
  `follower_id` int(11) NOT NULL,
  `followed_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

-- ============================================
-- MODULE: Investissement
-- Tables: investment_opportunity, investment_offer
-- ============================================

--
-- Structure de la table `investment_opportunity`
--

CREATE TABLE `investment_opportunity` (
  `id` int(11) NOT NULL,
  `target_amount` decimal(15,2) NOT NULL,
  `description` text DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `status` enum('OPEN','CLOSED','FUNDED') DEFAULT 'OPEN',
  `project_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `investment_offer`
--

CREATE TABLE `investment_offer` (
  `id` int(11) NOT NULL,
  `proposed_amount` decimal(15,2) NOT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
  `investor_id` int(11) NOT NULL,
  `opportunity_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

-- ============================================
-- MODULE: Apprentissage & Gamification
-- Tables: cours, progression, badge
-- ============================================

--
-- Structure de la table `cours`
--

CREATE TABLE `cours` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `categorie` varchar(100) NOT NULL DEFAULT 'Général',
  `niveau_difficulte` enum('DEBUTANT','INTERMEDIAIRE','AVANCE','EXPERT') NOT NULL DEFAULT 'DEBUTANT',
  `points_xp` int(11) NOT NULL DEFAULT 100,
  `duree_estimee` int(11) DEFAULT 60 COMMENT 'Durée en minutes',
  `image_url` varchar(500) DEFAULT NULL,
  `certification` tinyint(1) DEFAULT 0,
  `actif` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `cours`
--

INSERT INTO `cours` (`id`, `titre`, `description`, `categorie`, `niveau_difficulte`, `points_xp`, `duree_estimee`, `certification`) VALUES
(1, 'Introduction à l''Entrepreneuriat', 'Découvrez les bases de la création d''entreprise et lancez votre projet.', 'Entrepreneuriat', 'DEBUTANT', 100, 120, 0),
(2, 'Business Plan Professionnel', 'Apprenez à rédiger un business plan convaincant pour les investisseurs.', 'Entrepreneuriat', 'INTERMEDIAIRE', 200, 180, 1),
(3, 'Marketing Digital Fondamentaux', 'Maîtrisez les outils du marketing en ligne pour promouvoir votre entreprise.', 'Marketing', 'DEBUTANT', 150, 150, 0),
(4, 'Gestion Financière pour Startups', 'Gérez efficacement les finances de votre startup.', 'Finance', 'AVANCE', 300, 240, 1),
(5, 'Leadership et Management', 'Développez vos compétences en leadership pour diriger une équipe.', 'Management', 'INTERMEDIAIRE', 250, 200, 1);

-- --------------------------------------------------------

--
-- Structure de la table `progression`
--

CREATE TABLE `progression` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `cours_id` int(11) NOT NULL,
  `pourcentage` decimal(5,2) DEFAULT 0.00,
  `points_xp` int(11) DEFAULT 0,
  `niveau` int(11) DEFAULT 1,
  `etat` enum('NON_COMMENCE','EN_COURS','COMPLETE','CERTIFIE') DEFAULT 'NON_COMMENCE',
  `date_debut` timestamp NOT NULL DEFAULT current_timestamp(),
  `date_obtention` timestamp NULL DEFAULT NULL COMMENT 'Date de complétion du cours',
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `progression`
--

INSERT INTO `progression` (`id`, `user_id`, `cours_id`, `pourcentage`, `points_xp`, `niveau`, `etat`, `date_obtention`) VALUES
(1, 1, 1, 100.00, 100, 2, 'COMPLETE', '2026-02-08 00:00:00'),
(2, 1, 2, 60.00, 80, 1, 'EN_COURS', NULL),
(3, 1, 3, 25.00, 30, 1, 'EN_COURS', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `badge`
--

CREATE TABLE `badge` (
  `id` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `icone` varchar(50) DEFAULT '🏆',
  `condition_obtention` text NOT NULL COMMENT 'Description de la condition pour obtenir le badge',
  `points_requis` int(11) DEFAULT 0 COMMENT 'Points XP requis pour débloquer',
  `cours_requis` int(11) DEFAULT 0 COMMENT 'Nombre de cours à compléter',
  `niveau_requis` int(11) DEFAULT 0 COMMENT 'Niveau minimum requis',
  `categorie` varchar(50) DEFAULT 'Général',
  `rarete` enum('COMMUN','RARE','EPIQUE','LEGENDAIRE') DEFAULT 'COMMUN',
  `actif` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `badge`
--

INSERT INTO `badge` (`id`, `nom`, `description`, `icone`, `condition_obtention`, `points_requis`, `cours_requis`, `niveau_requis`, `categorie`, `rarete`) VALUES
(1, 'Premier Pas', 'Bienvenue dans l''aventure ! Vous avez complété votre premier cours.', '🎯', 'Compléter 1 cours', 0, 1, 0, 'Progression', 'COMMUN'),
(2, 'Explorateur', 'Vous avez exploré 3 cours différents. Continuez votre apprentissage !', '🗺️', 'Compléter 3 cours', 0, 3, 0, 'Progression', 'COMMUN'),
(3, 'Étudiant Assidu', 'Vous avez atteint 500 points XP. Bravo pour votre persévérance !', '📚', 'Atteindre 500 XP', 500, 0, 0, 'XP', 'RARE'),
(4, 'Expert en Devenir', 'Vous avez atteint 1000 points XP. Vous êtes sur la bonne voie !', '⭐', 'Atteindre 1000 XP', 1000, 0, 0, 'XP', 'RARE'),
(5, 'Niveau 5', 'Vous avez atteint le niveau 5. Vous progressez rapidement !', '🏅', 'Atteindre le niveau 5', 0, 0, 5, 'Niveau', 'EPIQUE'),
(6, 'Maître Entrepreneur', 'Vous avez complété 5 cours. Vous êtes un vrai professionnel !', '🎓', 'Compléter 5 cours', 0, 5, 0, 'Progression', 'EPIQUE'),
(7, 'Légende', 'Vous avez atteint 5000 points XP. Vous êtes une légende !', '👑', 'Atteindre 5000 XP', 5000, 0, 0, 'XP', 'LEGENDAIRE'),
(8, 'Certifié', 'Vous avez obtenu votre première certification.', '📜', 'Obtenir une certification', 0, 0, 0, 'Certification', 'RARE');

-- --------------------------------------------------------

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `thread_id` (`thread_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `demande_mentorat`
--
ALTER TABLE `demande_mentorat`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `donnees_business`
--
ALTER TABLE `donnees_business`
  ADD PRIMARY KEY (`id`),
  ADD KEY `projet_id` (`projet_id`);

--
-- Index pour la table `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `created_by` (`created_by`);

--
-- Index pour la table `event_participants`
--
ALTER TABLE `event_participants`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `event_id` (`event_id`,`user_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `groups`
--
ALTER TABLE `groups`
  ADD PRIMARY KEY (`id`),
  ADD KEY `group_admin_id` (`group_admin_id`);

--
-- Index pour la table `group_join_request`
--
ALTER TABLE `group_join_request`
  ADD PRIMARY KEY (`id`),
  ADD KEY `group_id` (`group_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `group_members`
--
ALTER TABLE `group_members`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `group_id` (`group_id`,`user_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `post_reactions`
--
ALTER TABLE `post_reactions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `post_id` (`post_id`,`user_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `projet`
--
ALTER TABLE `projet`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `session_mentorat`
--
ALTER TABLE `session_mentorat`
  ADD PRIMARY KEY (`id`),
  ADD KEY `demande_id` (`demande_id`);

--
-- Index pour la table `threads`
--
ALTER TABLE `threads`
  ADD PRIMARY KEY (`id`),
  ADD KEY `group_id` (`group_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Index pour la table `user_connection`
--
ALTER TABLE `user_connection`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `follower_id` (`follower_id`,`followed_id`),
  ADD KEY `followed_id` (`followed_id`);

--
-- Index pour la table `investment_opportunity`
--
ALTER TABLE `investment_opportunity`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_opportunity_project` (`project_id`),
  ADD KEY `idx_opportunity_status` (`status`),
  ADD KEY `idx_opportunity_deadline` (`deadline`);

--
-- Index pour la table `investment_offer`
--
ALTER TABLE `investment_offer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_offer_investor` (`investor_id`),
  ADD KEY `idx_offer_opportunity` (`opportunity_id`),
  ADD KEY `idx_offer_status` (`status`);

--
-- Index pour la table `cours`
--
ALTER TABLE `cours`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_categorie` (`categorie`),
  ADD KEY `idx_niveau` (`niveau_difficulte`),
  ADD KEY `idx_actif` (`actif`);

--
-- Index pour la table `progression`
--
ALTER TABLE `progression`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_cours` (`user_id`, `cours_id`),
  ADD KEY `idx_user_progression` (`user_id`),
  ADD KEY `idx_cours_progression` (`cours_id`),
  ADD KEY `idx_etat` (`etat`);

--
-- Index pour la table `badge`
--
ALTER TABLE `badge`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `nom` (`nom`),
  ADD KEY `idx_rarete` (`rarete`),
  ADD KEY `idx_actif_badge` (`actif`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `comments`
--
ALTER TABLE `comments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- AUTO_INCREMENT pour la table `demande_mentorat`
--
ALTER TABLE `demande_mentorat`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `donnees_business`
--
ALTER TABLE `donnees_business`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT pour la table `events`
--
ALTER TABLE `events`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `event_participants`
--
ALTER TABLE `event_participants`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT pour la table `groups`
--
ALTER TABLE `groups`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT pour la table `group_join_request`
--
ALTER TABLE `group_join_request`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `group_members`
--
ALTER TABLE `group_members`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `posts`
--
ALTER TABLE `posts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `post_reactions`
--
ALTER TABLE `post_reactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `projet`
--
ALTER TABLE `projet`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `session_mentorat`
--
ALTER TABLE `session_mentorat`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `threads`
--
ALTER TABLE `threads`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=41;

--
-- AUTO_INCREMENT pour la table `user_connection`
--
ALTER TABLE `user_connection`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `investment_opportunity`
--
ALTER TABLE `investment_opportunity`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `investment_offer`
--
ALTER TABLE `investment_offer`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `cours`
--
ALTER TABLE `cours`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `progression`
--
ALTER TABLE `progression`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `badge`
--
ALTER TABLE `badge`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`thread_id`) REFERENCES `threads` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `events`
--
ALTER TABLE `events`
  ADD CONSTRAINT `events_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `event_participants`
--
ALTER TABLE `event_participants`
  ADD CONSTRAINT `event_participants_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `event_participants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `groups`
--
ALTER TABLE `groups`
  ADD CONSTRAINT `groups_ibfk_1` FOREIGN KEY (`group_admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `group_join_request`
--
ALTER TABLE `group_join_request`
  ADD CONSTRAINT `group_join_request_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `group_join_request_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `group_members`
--
ALTER TABLE `group_members`
  ADD CONSTRAINT `group_members_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `group_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `post_reactions`
--
ALTER TABLE `post_reactions`
  ADD CONSTRAINT `post_reactions_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_reactions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `session_mentorat`
--
ALTER TABLE `session_mentorat`
  ADD CONSTRAINT `session_mentorat_ibfk_1` FOREIGN KEY (`demande_id`) REFERENCES `demande_mentorat` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `threads`
--
ALTER TABLE `threads`
  ADD CONSTRAINT `threads_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `threads_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `user_connection`
--
ALTER TABLE `user_connection`
  ADD CONSTRAINT `user_connection_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_connection_ibfk_2` FOREIGN KEY (`followed_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Note: pas de FK vers `projet` car la table utilise MyISAM (ne supporte pas les FK)
-- La relation est gérée au niveau applicatif (DAO)
--

--
-- Contraintes pour la table `investment_offer`
--
ALTER TABLE `investment_offer`
  ADD CONSTRAINT `fk_offer_investor` FOREIGN KEY (`investor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_offer_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `investment_opportunity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `progression`
--
ALTER TABLE `progression`
  ADD CONSTRAINT `fk_progression_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_progression_cours` FOREIGN KEY (`cours_id`) REFERENCES `cours` (`id`) ON DELETE CASCADE;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
