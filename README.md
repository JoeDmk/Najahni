# 🚀 NAJAHNI — Plateforme Fintech d'Entrepreneuriat & Investissement

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.3.0-blue)]()
[![Tests](https://img.shields.io/badge/Tests-223%20passing-brightgreen)]()
[![License](https://img.shields.io/badge/License-Academic-lightgrey)]()

> Application desktop JavaFX pour la gestion d'investissements, l'analyse de risques par IA, et la mise en relation intelligente investisseurs–entrepreneurs.

---

## 📋 Table des Matières

- [Vue d'ensemble](#-vue-densemble)
- [Fonctionnalités](#-fonctionnalités)
- [Architecture technique](#-architecture-technique)
- [APIs & Services externes](#-apis--services-externes)
- [Installation & Configuration](#-installation--configuration)
- [Lancement](#-lancement)
- [Tests](#-tests)
- [Structure du projet](#-structure-du-projet)
- [Design Patterns](#-design-patterns)

---

## 🎯 Vue d'ensemble

**NAJAHNI** est une plateforme fintech desktop complète permettant de :

- **Gérer des projets entrepreneuriaux** avec workflow de validation
- **Créer et suivre des opportunités d'investissement** avec scoring de risque IA
- **Réaliser des paiements sécurisés** via l'API Stripe
- **Analyser les risques** grâce à un moteur ML + données économiques temps réel
- **Matcher intelligemment** investisseurs et projets via un algorithme de scoring multi-critères
- **Signer des smart contracts** avec intégrité SHA-256
- **Visualiser des métriques financières avancées** (HHI, Shannon Entropy, CAGR, Sharpe Ratio)

### Rôles utilisateurs

| Rôle | Accès | Fonctionnalités |
|------|-------|-----------------|
| **ADMIN** | Back-office | Dashboard global, CRUD complet, gestion économique |
| **ENTREPRENEUR** | Front-office | Création projets, gestion opportunités, inbox notifications |
| **INVESTOR** | Front-office | Investir, paiement Stripe, portfolio, contrats, matching IA |
| **MENTOR** | Back-office | Suivi des cours et progression |

---

## ✨ Fonctionnalités

### 🔐 Module Investissement — Fonctionnalités avancées

| # | Fonctionnalité | Description | Technologies |
|---|----------------|-------------|--------------|
| 1 | **Smart Contracts** | Création automatique, hachage SHA-256, double signature (investisseur + entrepreneur), vérification d'intégrité | SHA-256, JDBC |
| 2 | **AI Matching** | Algorithme de mise en relation investisseur ↔ opportunité avec scoring pondéré (secteur 35%, budget 25%, risque 25%, horizon 15%) | Algorithme propriétaire |
| 3 | **Portfolio Analytics** | Dashboard KPI avancé : Indice HHI, Entropie de Shannon, Score de diversification, Risque pondéré, Matrice Risque-Rendement | Métriques financières |
| 4 | **Comparateur** | Comparaison côte-à-côte des opportunités d'investissement avec scoring visuel et détection du meilleur choix | JavaFX Canvas |
| 5 | **Système de Notation** | Notation 1-5 étoiles par investisseur avec moyennes pondérées et affichage visuel | JDBC, UI |
| 6 | **Simulateur What-If** | Projection multi-scénario : CAGR, Ratio de Sharpe, volatilité, Monte Carlo simplifié, drawdown | Mathématiques financières |
| 7 | **Paiement Stripe** | Intégration complète de l'API Stripe pour le paiement des investissements | API Stripe REST |
| 8 | **Analyse de Risque IA** | Scoring automatique avec gauge animée, analyse approfondie via LLM, recommandations | HuggingFace + Gemini |
| 9 | **Export PDF** | Génération de rapports d'investissement avec prévisualisation in-app | OpenPDF + PDFBox |
| 10 | **Chatbot IA** | Widget flottant avec assistant IA contextuel et quick actions | HuggingFace Llama 3.2 |

### 📊 Module Économique

- Dashboard économique temps réel (PIB, inflation, taux de change)
- Comparaison multi-pays avec données World Bank
- Gauge de risque économique composite

### 📚 Module Apprentissage & Gamification

- Gestion des cours avec niveaux et types
- Suivi de progression par utilisateur
- Système de badges et gamification

---

## 🏗 Architecture Technique

### Stack

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 17 |
| UI Framework | JavaFX (FXML + CSS) | 17.0.2 |
| Base de données | MySQL | 8.3.0 |
| Connecteur DB | mysql-connector-j | 8.3.0 |
| Build Tool | Maven | 3.9.12 |
| PDF Generation | OpenPDF (LibrePDF) | 1.3.30 |
| PDF Preview | Apache PDFBox | 2.0.31 |
| Tests | JUnit 5 Jupiter | 5.10.2 |
| Mocking | Mockito | 5.11.0 |
| Module System | JPMS (module-info.java) | — |

### Architecture logicielle

```
┌─────────────────────────────────────────────────┐
│                    JavaFX UI                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │   FXML   │  │   CSS    │  │  Controllers │  │
│  └──────────┘  └──────────┘  └──────┬───────┘  │
│                                      │          │
│  ┌───────────────────────────────────▼────────┐ │
│  │             Services Layer                 │ │
│  │  ┌─────────┐ ┌──────────┐ ┌────────────┐  │ │
│  │  │  CRUD   │ │ API      │ │  Business  │  │ │
│  │  │Services │ │Services  │ │  Logic     │  │ │
│  │  └─────────┘ └──────────┘ └────────────┘  │ │
│  └───────────────────────────────────┬────────┘ │
│                                      │          │
│  ┌───────────────────────────────────▼────────┐ │
│  │              Models Layer                  │ │
│  │  Entities, Enums, Data Classes             │ │
│  └───────────────────────────────────┬────────┘ │
│                                      │          │
│  ┌───────────────────────────────────▼────────┐ │
│  │          MySQL (JDBC direct)               │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
         │              │              │
    ┌────▼────┐   ┌─────▼─────┐  ┌────▼────────┐
    │ Stripe  │   │HuggingFace│  │ World Bank  │
    │  API    │   │ + Gemini  │  │ + Open ER   │
    └─────────┘   └───────────┘  └─────────────┘
```

---

## 🌐 APIs & Services Externes

| # | API | Endpoint | Utilisation | Service |
|---|-----|----------|-------------|---------|
| 1 | **Stripe** | `api.stripe.com/v1/payment_intents` | Paiement des investissements | `PaymentService` |
| 2 | **HuggingFace** (Llama 3.2) | `router.huggingface.co/v1/chat/completions` | Chatbot IA + analyse de risque | `GeminiService` |
| 3 | **Google Gemini** (2.0 Flash) | `generativelanguage.googleapis.com` | Fallback IA si HuggingFace down | `GeminiService` |
| 4 | **Open Exchange Rates** | `open.er-api.com/v6/latest/EUR` | Taux de change EUR/USD/TND | `EconomicApiService` |
| 5 | **World Bank** | `api.worldbank.org/v2` | PIB et inflation par pays | `EconomicApiService` |
| 6 | **Open-Meteo** | `api.open-meteo.com/v1/forecast` | Facteur externe pour risque | `RiskService` |

> Toutes les intégrations utilisent `java.net.http.HttpClient` natif (aucune lib HTTP tierce).

---

## ⚙ Installation & Configuration

### Prérequis

- **Java JDK 17+**
- **Maven 3.8+**
- **MySQL 8.0+**

### 1. Cloner le dépôt

```bash
git clone https://github.com/JoeDmk/Najahni.git
cd Najahni
git checkout investissement
```

### 2. Configurer la base de données

```sql
-- Créer la base et importer le schéma
mysql -u root -p < sql/najahni_db_combined.sql
```

Ou exécuter manuellement `sql/najahni_db_combined.sql` dans MySQL Workbench.

**Configuration de connexion** dans `DBConnection.java` :
```java
URL  = "jdbc:mysql://localhost:3306/najahni_db"
USER = "root"
PASS = ""
```

### 3. Installer les dépendances

```bash
mvn clean install -DskipTests
```

---

## 🚀 Lancement

```bash
mvn javafx:run
```

---

## 🧪 Tests

```bash
# Exécuter les 223 tests unitaires
mvn clean test
```

### Couverture des tests

| Fichier de test | Tests | Couverture |
|----------------|-------|-----------|
| `EconomicRiskEngineTest` | ~60 | Moteur économique (GDP, inflation, forex, JSON parsing) |
| `InvestmentModelTest` | ~53 | Modèles InvestmentOffer, Opportunity, Profile, Rating, Enums |
| `RiskCalculatorTest` | ~34 | Scoring de risque (montant, durée, facteur API) |
| `MatchingAlgorithmTest` | ~26 | Matching IA (secteur, budget, risque, horizon) |
| `ContractServiceTest` | ~20 | Smart contracts, SHA-256, intégrité, signatures |
| `InvestmentOfferServiceTest` | ~15 | Service offres d'investissement |
| `RiskMLTest` | ~9 | Module Machine Learning |
| `InvestmentOpportunityServiceTest` | ~6 | Service opportunités |
| **Total** | **223** | **✅ 100% passing** |

---

## 📁 Structure du Projet

```
src/main/java/com/najahni/
├── MainApp.java                          # Point d'entrée JavaFX
├── controllers/                          # 17 contrôleurs
│   ├── LoginController.java              # Authentification + routage par rôle
│   ├── MainController.java               # Navigation back-office (sidebar)
│   ├── FrontOfficeController.java        # Navigation front-office + inbox
│   ├── AdvancedInvestmentController.java  # Dashboard avancé (5 onglets)
│   ├── FrontRiskAnalysisController.java  # Analyse risque IA interactive
│   ├── AIChatWidget.java                 # Chatbot IA flottant
│   ├── EconomicDashboardController.java  # Dashboard économique temps réel
│   ├── FrontOpportunitiesController.java # Gestion opportunités front-office
│   ├── FrontOffersController.java        # Gestion offres + paiement Stripe
│   ├── FrontPortfolioController.java     # Portfolio investisseur + PDF
│   ├── DashboardController.java          # Dashboard admin back-office
│   ├── UserController.java               # CRUD utilisateurs
│   ├── ProjectController.java            # CRUD projets
│   ├── InvestmentOpportunityController.java # CRUD opportunités (BO)
│   ├── InvestmentOfferController.java    # CRUD offres (BO)
│   ├── CoursController.java              # CRUD cours
│   └── ApprentissageController.java      # Apprentissage + gamification
├── services/                             # 21 services
│   ├── PaymentService.java               # API Stripe (paiement)
│   ├── GeminiService.java                # HuggingFace Llama + Gemini (dual AI)
│   ├── ContractService.java              # Smart Contracts SHA-256
│   ├── InvestmentMatchingService.java    # Matching investisseur ↔ projet
│   ├── EconomicApiService.java           # APIs World Bank + Exchange Rates
│   ├── EconomicRiskEngine.java           # Moteur risque économique composite
│   ├── RiskCalculator.java               # Calcul score de risque
│   ├── RiskService.java                  # Orchestrateur risque + Open-Meteo
│   ├── InvestmentPDFService.java         # Export PDF (OpenPDF + PDFBox)
│   ├── CurrencyService.java              # Conversion de devises
│   ├── RatingService.java                # Système de notation
│   ├── SessionManager.java               # Gestion session utilisateur
│   └── ...                               # CRUD services (User, Project, etc.)
├── models/                               # 19 entités + enums
└── utils/                                # 5 utilitaires
    ├── DBConnection.java                 # Singleton MySQL
    ├── AnimationUtils.java               # Animations UI (fade, scale)
    ├── PDFPreviewPopup.java              # Preview PDF in-app
    ├── AlertUtils.java                   # Helpers d'alertes JavaFX
    └── WrappedTextCellFactory.java       # Cell factory pour TableView

src/main/resources/
├── fxml/                                 # 16 vues FXML
├── css/styles.css                        # Charte graphique premium (~2600 lignes)
└── ml/                                   # Modèle ML sérialisé

src/test/java/                            # 7 fichiers — 223 tests JUnit 5
sql/                                       # Schémas SQL (2 fichiers)
docs/                                      # Documentation technique
```

---

## 🎨 Design Patterns

| Pattern | Implémentation |
|---------|---------------|
| **Singleton** | `DBConnection` — instance unique de connexion MySQL |
| **MVC** | FXML (View) ↔ Controller ↔ Service ↔ Model |
| **Service Layer** | Couche métier séparée des contrôleurs |
| **DAO** | Accès données abstrait dans les services |
| **Strategy** | Scoring multi-critères dans `InvestmentMatchingService` |
| **Observer** | Listeners JavaFX (sliders, properties) pour réactivité |
| **Builder** | Construction fluide des PDFs dans `InvestmentPDFService` |
| **Fallback/Circuit Breaker** | HuggingFace → Gemini dans `GeminiService` |

---

## 📊 Métriques Financières Implémentées

| Métrique | Description | Module |
|----------|-------------|--------|
| **HHI** (Herfindahl-Hirschman Index) | Mesure la concentration sectorielle du portefeuille (0–10000) | KPI Dashboard |
| **Entropie de Shannon** | Qualité de la diversification sectorielle | KPI Dashboard |
| **Score de Diversification** | Ratio entropie/entropie maximale (0–100%) | KPI Dashboard |
| **Risque Pondéré** | Moyenne des risques pondérée par montant investi | KPI Dashboard |
| **CAGR** | Taux de croissance annuel composé | Simulateur |
| **Ratio de Sharpe** | Performance ajustée au risque vs taux sans risque | Simulateur |
| **Drawdown Maximum** | Estimation de la perte potentielle maximale | Simulateur |
| **Score Ajusté Risque** | Contribution risque-pondérée par position | Matrice R/R |

---

## 👥 Informations Académiques

- **Projet** : PIDEV — NAJAHNI
- **Sprint** : Java Desktop (JDBC)
- **Module** : Investissement
- **Branche** : `investissement`

---

**NAJAHNI** — Empowering Entrepreneurs, Connecting Investors 🚀
