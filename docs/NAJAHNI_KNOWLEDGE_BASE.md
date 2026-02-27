# NAJAHNI — Comprehensive Knowledge Base
## JavaFX Fintech Desktop Application — AI Training Context

---

## 1. APPLICATION OVERVIEW

**NAJAHNI** is a JavaFX 17 desktop application for entrepreneurship and investment management, built for the Tunisian market. It connects entrepreneurs with investors, provides AI-powered risk analysis, course-based learning with gamification, and integrates Stripe for payments.

- **Language**: Java 17
- **UI Framework**: JavaFX 17.0.2
- **Database**: MySQL 8.3.0 (localhost:3306, database `najahni_db`, user=root, no password)
- **Data Access**: Direct JDBC (no ORM, no Hibernate)
- **Build Tool**: Maven with `javafx-maven-plugin 0.0.8`
- **Entry Point**: `com.najahni.MainApp`
- **Main Class**: `MainApp extends Application`
- **Module**: `com.najahni` (uses `module-info.java`)

### Key Dependencies (pom.xml)
| Dependency | Version | Purpose |
|---|---|---|
| javafx-controls | 17.0.2 | JavaFX UI controls |
| javafx-fxml | 17.0.2 | FXML view loading |
| mysql-connector-j | 8.3.0 | MySQL JDBC driver |
| junit-jupiter | 5.10.2 | Unit testing |
| mockito-core | 5.11.0 | Test mocking |
| java.net.http (JDK) | — | HTTP client for APIs |

**No external JSON library** — all JSON parsing is done manually with string operations throughout the codebase.

### Application Startup Flow
1. `MainApp.main()` → `launch(args)`
2. `MainApp.start(Stage)` → loads `/fxml/LoginView.fxml` (900×600), applies `/css/styles.css`
3. `LoginController` authenticates via `UserService.authenticate(email, password)`
4. Role-based routing:
   - `INVESTOR` or `ENTREPRENEUR` → `/fxml/FrontOfficeView.fxml` (1200×700), title "Espace Investisseur" or "Espace Entrepreneur"
   - `ADMIN` or `MENTOR` → `/fxml/MainView.fxml` (1200×700), title "Back-Office"
5. `SessionManager.login(user)` stores the logged-in user

---

## 2. ARCHITECTURE & NAVIGATION

### 2.1 View-Controller Mapping (15 FXML views)

| FXML File | Controller | Role | Description |
|---|---|---|---|
| `LoginView.fxml` | `LoginController` | All | Login form (email/password) |
| `MainView.fxml` | `MainController` | Admin/Mentor | BackOffice shell with sidebar navigation |
| `DashboardView.fxml` | `DashboardController` | Admin/Mentor | Stats dashboard with charts |
| `UserView.fxml` | `UserController` | Admin/Mentor | User CRUD management |
| `ProjectView.fxml` | `ProjectController` | Admin/Mentor | Project CRUD management |
| `InvestmentView.fxml` | `InvestmentOpportunityController` | Admin/Mentor | Opportunity CRUD + risk scoring |
| `InvestmentOfferView.fxml` | `InvestmentOfferController` | Admin/Mentor | Offer CRUD + Stripe payment |
| `CoursView.fxml` | `CoursController` | Admin/Mentor | Course CRUD management |
| `ApprentissageView.fxml` | `ApprentissageController` | Admin/Mentor | Learning dashboard + gamification |
| `EconomicDashboardView.fxml` | `EconomicDashboardController` | Admin/Mentor | Economic analysis + charts |
| `FrontOfficeView.fxml` | `FrontOfficeController` | Investor/Entrepreneur | FrontOffice shell with tabs |
| `FrontOpportunitiesView.fxml` | `FrontOpportunitiesController` | Investor/Entrepreneur | Browse/create opportunities |
| `FrontOffersView.fxml` | `FrontOffersController` | Investor/Entrepreneur | Submit offers + Stripe payment |
| `FrontRiskAnalysisView.fxml` | `FrontRiskAnalysisController` | Investor/Entrepreneur | Interactive risk calculator |
| `FrontPortfolioView.fxml` | `FrontPortfolioController` | Investor/Entrepreneur | Paid investments portfolio |

### 2.2 Navigation Flows

**BackOffice (MainController)**: Sidebar buttons load views into `contentArea` (StackPane) with fade+slide transitions (300ms):
- `showDashboard()` → DashboardView.fxml
- `showUsers()` → UserView.fxml
- `showProjects()` → ProjectView.fxml
- `showOpportunities()` → InvestmentView.fxml
- `showOffers()` → InvestmentOfferView.fxml
- `showCours()` → CoursView.fxml
- `showApprentissage()` → ApprentissageView.fxml
- `showEconomicDashboard()` → EconomicDashboardView.fxml
- `openFrontOffice()` → replaces entire scene with FrontOfficeView.fxml
- `handleLogout()` → LoginView.fxml (900×600)

**FrontOffice (FrontOfficeController)**: Tab buttons load views into `foContentArea` (StackPane):
- `showOpportunities()` → FrontOpportunitiesView.fxml
- `showOffers()` → FrontOffersView.fxml (can pass `opportunityId` for preselection)
- `showRiskAnalysis()` → FrontRiskAnalysisView.fxml
- `showPortfolio()` → FrontPortfolioView.fxml
- `goBackOffice()` → MainView.fxml
- **Inbox** (entrepreneur only): Sliding panel from right (380px), shows offers on entrepreneur's projects
- **AIChatWidget**: Floating chatbot attached to `foContentArea`

### 2.3 Role-Aware UI Behavior

**FrontOfficeController role adaptations**:
- **ENTREPRENEUR**: Hides "Mes Offres" and "Portefeuille" tabs, shows Inbox button, subtitle "Espace Entrepreneur"
- **INVESTOR**: Hides Inbox button, shows all investment tabs

**FrontOpportunitiesController role adaptations**:
- **ENTREPRENEUR**: Shows create-opportunity form (newOppPane visible), loads their projects, cards show "En attente d'offres"
- **INVESTOR**: Cards show "💰 Investir" button on OPEN opportunities

---

## 3. DATA MODELS (8 Entity Classes)

### 3.1 User
**File**: `com.najahni.models.User`
**DB Table**: `user`

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `name` | String | `firstname` + `lastname` | Combined in Java, split in DB |
| `email` | String | `email` (UNIQUE) | |
| `password` | String | `password` | SHA-256 hashed |
| `role` | Role | `role` (VARCHAR) | Stored as display name |
| `createdAt` | LocalDateTime | `created_at` | |
| `updatedAt` | LocalDateTime | `updated_at` | |

**Validation**: name 2-100 chars, valid email regex, password ≥ 6 chars.

### 3.2 Project
**File**: `com.najahni.models.Project`
**DB Table**: `projet` (MyISAM engine — no FK support)

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `title` | String | `title` (VARCHAR 255) | |
| `description` | String | `description` (TEXT) | |
| `sector` | String | `sector` (VARCHAR 100) | |
| `status` | ProjectStatus | `status` | Enum string value |
| `entrepreneurId` | int | `entrepreneur_id` | References user.id (app-level) |
| `entrepreneurName` | String | — | Transient, from JOIN |
| `createdAt` | LocalDateTime | `created_at` | |
| `updatedAt` | LocalDateTime | `updated_at` | |

**Sector Options**: Technologie, Santé, Finance, Éducation, Énergie, Agriculture, Industrie, Commerce, Immobilier, Transport, Divertissement, Autre.

### 3.3 InvestmentOpportunity
**File**: `com.najahni.models.InvestmentOpportunity`
**DB Table**: `investment_opportunity`

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `targetAmount` | BigDecimal | `target_amount` (DECIMAL 15,2) | |
| `description` | String | `description` (TEXT) | |
| `deadline` | LocalDate | `deadline` (DATE) | |
| `status` | OpportunityStatus | `status` | OPEN/CLOSED/FUNDED |
| `projectId` | int | `project_id` (INDEX) | |
| `riskScore` | Double | `risk_score` (DOUBLE, nullable) | 0-100 |
| `riskLabel` | String | `risk_label` (VARCHAR 100) | e.g. "🟢 25/100 (Faible)" |
| `projectTitle` | String | — | Transient, from JOIN |
| `createdAt` | LocalDateTime | `created_at` | |
| `updatedAt` | LocalDateTime | `updated_at` | |

**Risk Level Thresholds**: ≤33 = "LOW" / "Faible", ≤66 = "MEDIUM" / "Moyen", >66 = "HIGH" / "Élevé".

**Methods**: `getFormattedAmount()` → "12 345,67 €", `getFormattedRiskScore()` → "25/100", `getRiskLevel()` → "LOW"/"MEDIUM"/"HIGH".

### 3.4 InvestmentOffer
**File**: `com.najahni.models.InvestmentOffer`
**DB Table**: `investment_offer`

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `proposedAmount` | BigDecimal | `proposed_amount` (DECIMAL 15,2) | |
| `status` | OfferStatus | `status` | PENDING/ACCEPTED/REJECTED |
| `investorId` | int | `investor_id` (FK → user.id) | |
| `opportunityId` | int | `opportunity_id` (FK → investment_opportunity.id) | |
| `paid` | boolean | `paid` (TINYINT 1) | Auto-migrated column |
| `paymentIntentId` | String | `payment_intent_id` (VARCHAR 255) | Auto-migrated column |
| `investorName` | String | — | Transient |
| `opportunityDescription` | String | — | Transient |
| `projectTitle` | String | — | Transient |
| `projectSector` | String | — | Transient |
| `createdAt` | LocalDateTime | `created_at` | |
| `updatedAt` | LocalDateTime | `updated_at` | |
| `paidAt` | LocalDateTime | `paid_at` | Auto-migrated column |

### 3.5 EconomicData
**File**: `com.najahni.models.EconomicData`
**No DB table** — runtime data from APIs.

| Field | Type | Notes |
|---|---|---|
| `exchangeRateEurUsd` | double | From Open Exchange Rates API |
| `exchangeRateEurTnd` | double | From Open Exchange Rates API |
| `gdpBillions` | double | From World Bank API |
| `inflationRate` | double | From World Bank API |
| `dataYear` | String | Year of economic data |
| `countryCode` | String | TN/FR/US/DE/MA/EG |
| `countryName` | String | Display name |
| `fetchTimestamp` | LocalDateTime | When data was fetched |
| `economicRiskFactor` | double | Computed composite factor |
| `dataAvailable` | boolean | Whether API succeeded |
| `errorMessage` | String | Error details if failed |

### 3.6 Cours
**File**: `com.najahni.models.Cours`
**DB Table**: `cours`

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `titre` | String | `titre` (VARCHAR 200) | |
| `description` | String | `description` (TEXT) | |
| `type` | TypeCours | `type` | VIDEO/TEXTE/QUIZ/MIXTE |
| `niveau` | NiveauCours | `niveau` | DEBUTANT/INTERMEDIAIRE/AVANCE/EXPERT |
| `certification` | boolean | `certification` (TINYINT 1) | |
| `pointsXP` | int | `points_xp` | Default by level |
| `dureeMinutes` | int | `duree_minutes` | |
| `imageUrl` | String | `image_url` | |
| `createurId` | int | `createur_id` | |
| `createurNom` | String | — | Transient |
| `createdAt` | LocalDateTime | `created_at` | |
| `updatedAt` | LocalDateTime | `updated_at` | |

**Default XP by Level**: DEBUTANT=100, INTERMEDIAIRE=200, AVANCE=350, EXPERT=500.
**Type Icons**: VIDEO="🎬", TEXTE="📖", QUIZ="❓", MIXTE="🔄".
**Level Icons**: DEBUTANT="🌱", INTERMEDIAIRE="📚", AVANCE="🎓", EXPERT="👑".

### 3.7 Progression
**File**: `com.najahni.models.Progression`
**DB Table**: `progression` (UNIQUE constraint on user_id + cours_id)

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `userId` | int | `user_id` (FK → user.id) | |
| `userNom` | String | — | Transient |
| `coursId` | int | `cours_id` (FK → cours.id) | |
| `coursTitre` | String | — | Transient |
| `pourcentage` | double | `pourcentage` | 0.0 to 100.0 |
| `pointsXP` | int | `points_xp` | |
| `niveau` | int | `niveau` | Computed from total XP |
| `etat` | EtatProgression | `etat` | NON_COMMENCE/EN_COURS/COMPLETE/CERTIFIE |
| `dateDebut` | LocalDateTime | `date_debut` | |
| `dateObtention` | LocalDateTime | `date_obtention` | Completion timestamp |
| `updatedAt` | LocalDateTime | `updated_at` | |

**Level Thresholds**: `SEUILS_NIVEAU = {0, 100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500}` — 11 levels total.

### 3.8 Badge
**File**: `com.najahni.models.Badge`
**DB Table**: `badge` (UNIQUE on `nom`)

| Field | Type | DB Column | Notes |
|---|---|---|---|
| `id` | int | `id` (PK, AUTO_INCREMENT) | |
| `nom` | String | `nom` (VARCHAR 100, UNIQUE) | |
| `description` | String | `description` (TEXT) | |
| `icone` | String | `icone` (VARCHAR 50) | Emoji icon |
| `condition` | String | `condition` (VARCHAR 255) | Human-readable condition |
| `pointsRequis` | int | `points_requis` | XP threshold |
| `coursRequis` | int | `cours_requis` | Completed courses threshold |
| `niveauRequis` | int | `niveau_requis` | Level threshold |
| `categorie` | String | `categorie` (VARCHAR 50) | |
| `rarete` | String | `rarete` (VARCHAR 30) | COMMUN/RARE/EPIQUE/LEGENDAIRE |
| `actif` | boolean | `actif` (TINYINT 1) | |
| `createdAt` | LocalDateTime | `created_at` | |

**Method**: `verifierCondition(int xpTotal, int coursCompletes, int niveau)` — checks if POINTS/COURS/NIVEAU conditions are met.

---

## 4. ENUMS (7 Enum Classes)

### 4.1 Role
```java
ENTREPRENEUR("ENTREPRENEUR"),  // DB value: "ENTREPRENEUR"
INVESTOR("INVESTISSEUR"),      // DB value: "INVESTISSEUR"
ADMIN("ADMIN"),                // DB value: "ADMIN"
MENTOR("MENTOR")               // DB value: "MENTOR"
```
**Methods**: `getDisplayName()`, `getDbValue()`, `fromDbValue(String)` — handles Java↔DB mapping for INVESTOR↔INVESTISSEUR.

### 4.2 ProjectStatus
```java
DRAFT("Brouillon"), PENDING("En attente"), APPROVED("Approuvé"),
REJECTED("Rejeté"), FUNDED("Financé")
```

### 4.3 OpportunityStatus
```java
OPEN("Ouverte"), CLOSED("Fermée"), FUNDED("Financée")
```

### 4.4 OfferStatus
```java
PENDING("En attente"), ACCEPTED("Acceptée"), REJECTED("Rejetée")
```

### 4.5 TypeCours
```java
VIDEO("Vidéo"), TEXTE("Texte"), QUIZ("Quiz"), MIXTE("Mixte")
```

### 4.6 NiveauCours
```java
DEBUTANT(1, "Débutant"), INTERMEDIAIRE(2, "Intermédiaire"),
AVANCE(3, "Avancé"), EXPERT(4, "Expert")
```

### 4.7 EtatProgression
```java
NON_COMMENCE("Non commencé"), EN_COURS("En cours"),
COMPLETE("Complété"), CERTIFIE("Certifié")
```

---

## 5. SERVICES (17 Service Classes)

### 5.1 SessionManager (Singleton)
**File**: `com.najahni.services.SessionManager`
- `getInstance()` → singleton
- `login(User user)` → stores current user
- `logout()` → clears session
- `getCurrentUser()` → User
- `isLoggedIn()` → boolean
- `getCurrentUserId()` → int

### 5.2 UserService
**File**: `com.najahni.services.UserService`
- **Password hashing**: SHA-256 via `MessageDigest`
- `authenticate(String email, String password)` → Optional<User>
- `createUser(User)` → User (hashes password, validates)
- `updateUser(User)` → boolean
- `deleteUser(int id)` → boolean
- `findById(int id)` → Optional<User>
- `findAll()` → List<User>
- `findByRole(Role)` → List<User>
- `getAllEntrepreneurs()` → List<User> (Role.ENTREPRENEUR)
- `getAllInvestors()` → List<User> (Role.INVESTOR)
- `emailExists(String email)` → boolean
- **Validation**: name 2-100 chars, valid email regex `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$`, password ≥ 6 chars

### 5.3 ProjectService
**File**: `com.najahni.services.ProjectService`
- `createProject(Project)` → Project (validates title, sector, entrepreneur role)
- `updateProject(Project)` → boolean
- `deleteProject(int id)` → boolean
- `findById(int id)` → Optional<Project>
- `findAll()` → List<Project> (JOINs with user table for entrepreneurName)
- `findByEntrepreneur(int entrepreneurId)` → List<Project>
- `findByStatus(ProjectStatus)` → List<Project>
- `findBySector(String sector)` → List<Project>
- `getAllSectors()` → List<String> (DISTINCT)
- `getProjectsForInvestment()` → List<Project> (status=APPROVED only)

### 5.4 InvestmentOpportunityService
**File**: `com.najahni.services.InvestmentOpportunityService`
- `createOpportunity(InvestmentOpportunity)` → InvestmentOpportunity
- `updateOpportunity(InvestmentOpportunity)` → boolean
- `deleteOpportunity(int id)` → boolean
- `findById(int id)` → Optional<InvestmentOpportunity>
- `findAll()` → List<InvestmentOpportunity> (JOINs with `projet`)
- `findByProject(int projectId)` → List<InvestmentOpportunity>
- `findByStatus(OpportunityStatus)` → List<InvestmentOpportunity>
- `countByStatus(OpportunityStatus)` → int
- `getTotalTargetAmount()` → BigDecimal
- `closeOpportunity(int id)` → boolean
- `markAsFunded(int id)` → boolean
- `updateRiskScore(int id, double score)` → boolean
- `updateRiskLabel(int id, String label)` → boolean

### 5.5 InvestmentOfferService
**File**: `com.najahni.services.InvestmentOfferService`
- `createOffer(InvestmentOffer)` → InvestmentOffer
- `updateOffer(InvestmentOffer)` → boolean
- `deleteOffer(int id)` → boolean
- `findById(int id)` → Optional<InvestmentOffer>
- `findAll()` → List<InvestmentOffer> (multi-table JOIN with user, opportunity, project)
- `findByOpportunity(int opportunityId)` → List<InvestmentOffer>
- `findByInvestor(int investorId)` → List<InvestmentOffer>
- `countByStatus(OfferStatus)` → int
- `getTotalAcceptedForOpportunity(int oppId)` → BigDecimal
- `acceptOffer(int id)` → boolean
- `rejectOffer(int id)` → boolean
- `markAsPaid(int id, String paymentIntentId)` → boolean
- `isOfferPaid(int id)` → boolean
- `findByProjectIds(List<Integer> projectIds)` → List<InvestmentOffer>
- **Auto-migration**: Creates `paid`, `payment_intent_id`, `paid_at` columns if missing

### 5.6 RiskCalculator (Simple Risk)
**File**: `com.najahni.services.RiskCalculator`
- **Formula**: `risk = (amountFactor × 0.4) + (durationFactor × 0.2) + (apiFactor × 0.4)`
- `MAX_AMOUNT` = 500,000€
- Amount factor: `min(amount / MAX_AMOUNT × 100, 100)`
- Duration factor: based on days until deadline (expired=100, ≤30d=20, ≤90d=40, ≤180d=60, ≤365d=80, >365d=90)
- API factor: from weather service (Open-Meteo)
- **Thresholds**: SEUIL_FAIBLE=33, SEUIL_MOYEN=66
- `getRiskStyle(Double score)` → CSS style string with color

### 5.7 EconomicRiskEngine (Enhanced Risk)
**File**: `com.najahni.services.EconomicRiskEngine`
- **Formula**: `risk = (amountFactor × 0.3) + (durationFactor × 0.2) + (economicFactor × 0.5)`
- **Economic Factor**: `(exchangeScore × 0.3) + (gdpScore × 0.3) + (inflationScore × 0.4)`
- `computeEconomicFactor(EconomicData)` → double
- `normalizeExchangeRate(double)` → double (0-100)
- `normalizeGdp(double)` → double (0-100)
- `normalizeInflation(double)` → double (0-100)
- `calculateFullRisk(BigDecimal amount, LocalDate deadline, EconomicData data)` → double
- `calculateRiskForOpportunity(InvestmentOpportunity, EconomicData)` → int
- Static helpers: `getRiskLevel(int)`, `getRiskColor(int)`, `getRiskEmoji(int)`, `getRecommendation(int)` (6 levels)
- **6 Recommendation Levels**: 0-15 (Très Faible), 16-33 (Faible), 34-50 (Modéré), 51-66 (Significatif), 67-85 (Élevé), 86-100 (Critique)

### 5.8 RiskService (Orchestrator)
**File**: `com.najahni.services.RiskService`
- Orchestrates weather API call + RiskCalculator
- Uses **Open-Meteo API** (Tunis coordinates: lat=36.80, lon=10.18)
- Weather → Risk mapping:
  - Temperature: 15-25°C=20, <5°C=70, >35°C=75
  - Wind: ≤10km/h=10, >60km/h=95
  - API factor = `temp × 0.7 + wind × 0.3`
- `calculateRisk(InvestmentOpportunity)` → `RiskResult`

### 5.9 RiskResult (Record)
**File**: `com.najahni.services.RiskResult`
```java
record RiskResult(int score, String level, String emoji)
```
- `getDisplay()` → "🟢 25/100 (Faible)"

### 5.10 EconomicApiService
**File**: `com.najahni.services.EconomicApiService`
- **3 External APIs**:
  1. Open Exchange Rates: `https://open.er-api.com/v6/latest/EUR` (free, no token)
  2. World Bank GDP: `https://api.worldbank.org/v2/country/{code}/indicator/NY.GDP.MKTP.CD?format=json&per_page=5`
  3. World Bank Inflation: `https://api.worldbank.org/v2/country/{code}/indicator/FP.CPI.TOTL.ZG?format=json&per_page=5`
- `fetchAllEconomicData(String countryCode)` → EconomicData
- **Fallback Values**: Tunisia GDP=46.7B, Inflation=8.3%

### 5.11 PaymentService (Stripe)
**File**: `com.najahni.services.PaymentService`
- **Stripe API** in test mode
- `isConfigured()` → boolean (checks for `sk_test_` or `sk_live_` key)
- `createPaymentIntent(long amountCents, String currency, String description)` → `CompletableFuture<PaymentResult>`
- Uses `pm_card_visa` for automatic confirmation
- Returns `PaymentResult` with `isSuccess()`, `getPaymentIntentId()`, `getErrorMessage()`

### 5.12 CurrencyService
**File**: `com.najahni.services.CurrencyService`
- Real-time exchange rates via `open.er-api.com`
- **Supported Currencies**: EUR, USD, TND, GBP, MAD
- **30-minute cache**
- `getExchangeRate(String from, String to)` → CompletableFuture<Double>
- `formatAmount(BigDecimal amount, String currency)` → String
- **Fallback rates** when API fails: EUR→USD=1.08, EUR→TND=3.35, EUR→GBP=0.86, EUR→MAD=10.85

### 5.13 DeadlineService
**File**: `com.najahni.services.DeadlineService`
- Static utility for deadline badge generation
- **Levels**: EXPIRED (<0 days), URGENT (≤7d), ATTENTION (≤15d), PROCHE (≤30d), OK (>30d)
- `getDeadlineBadge(LocalDate deadline)` → String (e.g., "⚠️ J-5 URGENT")
- `getDeadlineStyle(LocalDate deadline)` → CSS style with colors
- Colors: EXPIRED=#c0392b, URGENT=#e74c3c, ATTENTION=#f39c12, PROCHE=#3498db, OK=#27ae60

### 5.14 GeminiService (Hybrid AI)
**File**: `com.najahni.services.GeminiService`
- **Primary Backend**: HuggingFace Inference API → `meta-llama/Llama-3.2-3B-Instruct`
  - URL: `https://router.huggingface.co/v1/chat/completions`
  - Bearer token authentication
- **Fallback Backend**: Google Gemini 2.0 Flash
  - URL: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=...`
- **System Prompt**: NAJAHNI AI financial advisor, French language, Tunisian market context
- **Methods**:
  - `chat(String message)` → CompletableFuture<String>
  - `analyzeRisk(double score, String level, double amount, String deadline, String economicContext)` → CompletableFuture<String>
  - `generateProjectInsights(String projectName, String sector, String description, double amount, String deadline)` → CompletableFuture<String>
  - `clearHistory()` → resets conversation
  - `getBackendName()` → String (model name or "Gemini Cloud")
- **Max conversation turns**: 20 (trims oldest messages)
- **Timeout**: 30 seconds per request

### 5.15 CoursService
**File**: `com.najahni.services.CoursService`
- French method names: `creerCours`, `trouverParId`, `trouverTous`, `modifierCours`, `supprimerCours`
- `rechercherCours(String query)` → List<Cours>
- `trouverParNiveau(NiveauCours)` → List<Cours>
- `trouverParType(TypeCours)` → List<Cours>
- `compterTous()` → int
- `compterParNiveau(NiveauCours)` → int
- `compterCertifiants()` → int
- `calculerTotalXP()` → int

### 5.16 ProgressionService
**File**: `com.najahni.services.ProgressionService`
- `demarrerCours(int userId, int coursId)` → Progression (creates with 0%, NON_COMMENCE)
- `mettreAJourPourcentage(int progId, double pourcentage)` → boolean
- `completerCours(int progId)` → boolean (sets 100%, awards XP, triggers badge check)
- `trouverParUtilisateur(int userId)` → List<Progression>
- `getTotalXP(int userId)` → int
- `getNiveauGlobal(int userId)` → int (from SEUILS_NIVEAU)
- `getStatistiquesUtilisateur(int userId)` → int[] {totalXP, niveau, coursCompletes}
- `getLeaderboard(int limit)` → List<Object[]> {userId, name, totalXP, coursCompletes, niveauMax}

### 5.17 BadgeService
**File**: `com.najahni.services.BadgeService`
- CRUD: `creerBadge`, `trouverParId`, `trouverTous`, `modifierBadge`, `supprimerBadge`
- `findBadgesEligibles(int userId)` → List<Badge> (checks conditions against user stats)
- `verifierEtAttribuerBadges(int userId)` → List<Badge> (finds and awards new badges)
- `compterBadgesUtilisateur(int userId)` → int

---

## 6. CONTROLLERS — DETAILED BEHAVIOR

### 6.1 LoginController (125 lines)
**FXML**: `txtEmail` (TextField), `txtPassword` (PasswordField), `lblError` (Label), `btnLogin` (Button)
- Authenticates via `UserService.authenticate(email, password)`
- On success: `SessionManager.login(user)`, loads role-appropriate view (1200×700), fade-in (400ms)
- On failure: shows error in `lblError`
- Enter key: `txtEmail` → focuses password, `txtPassword` → triggers `handleLogin()`

### 6.2 MainController (250 lines)
**FXML**: `contentArea` (StackPane), 8 navigation buttons
- Stores `this` as `userData` on scene root for child controller access
- `loadView(String fxmlPath)`: FXMLLoader + fade(300ms) + slideUp(20px→0) via ParallelTransition
- Default view on init: DashboardView

### 6.3 FrontOfficeController (526 lines)
**FXML**: `foContentArea` (StackPane), `lblNavSubtitle`, 5 tab buttons
- **Inbox system** (entrepreneur only):
  - Sliding panel from right, 380px width
  - `foInboxOverlay` (semi-transparent) + `foInboxPanel`
  - Loads offers on entrepreneur's projects via `offerService.findByProjectIds()`
  - Each offer card has Accept/Decline buttons for PENDING offers
  - `acceptOffer(offer)` → `offerService.acceptOffer()` + refresh
  - `declineOffer(offer)` → `offerService.rejectOffer()` + refresh
- `showOffers(int opportunityId)`: Loads FrontOffersView and calls `controller.preselectOpportunity(id)`
- **AIChatWidget**: `AIChatWidget.attachTo(foContentArea)` called during init

### 6.4 DashboardController (500 lines)
**FXML**: Summary labels (users/projects/investments/deadlines), PieChart, BarChart, TableView (recent 10 projects)
- **PieChart**: Opportunity status distribution (Open=#27ae60, Closed=#e74c3c, Funded=#3498db)
- **BarChart**: Top 5 projects by total target amount (color: #0f3460)
- **Deadline stats**: Counts URGENT+EXPIRED and ATTENTION+PROCHE opportunities
- Staggered card animations on load (0/100/200/300ms delays)

### 6.5 UserController (305 lines)
**FXML**: TableView<User> (columns: Id, Name, Email, Role, Actions), form (name, email, password, role), search (text + role filter)
- **Search/Filter**: By text (name/email) and role dropdown (Tous/Entrepreneur/Investisseur)
- **Actions per row**: Edit (✏️), Delete (🗑️)
- **Save validation**: name required (≥2 chars), email required (regex), password required (≥6 chars), role required
- In edit mode: password field cleared with prompt "Entrer le nouveau mot de passe"
- Uses `WrappedTextCellFactory` for text columns

### 6.6 ProjectController (370 lines)
**FXML**: TableView<Project> (columns: Id, Title, Sector, Status, Entrepreneur, Actions), form (title, sector, status, entrepreneur, description), search (text + status + sector filters)
- **Entrepreneur combo**: Loads via `userService.getAllEntrepreneurs()`, shows "Name (email)"
- **Sector combo**: Editable with predefined options
- **Default status on add**: DRAFT
- Uses `WrappedTextCellFactory` for text columns

### 6.7 InvestmentOpportunityController (600 lines)
**FXML**: Summary cards (Open/Closed/Funded/TotalAmount), currency converter, TableView, form
- **Currency converter**: `cboCurrency` dropdown (EUR/USD/TND/GBP/MAD) + `lblConvertedTotal`
  - Uses `CurrencyService.getExchangeRate()` async
- **Table columns**: Id, TargetAmount, Description, Deadline, Status, Project, RiskScore, Actions
- **Deadline column**: Color-coded badges via `DeadlineService`
- **Risk Score column**: Color-coded via `RiskCalculator.getRiskStyle()`
- **Action buttons per row**: Edit(✏️), Risk(🎯), Close(🔒, OPEN only), Delete(🗑️)
- **`calculateRisk()`**: Confirmation popup → `RiskService.calculateRisk(opp)` → saves score + label to DB → result popup
- **Form validation**: project required, amount > 0, status required

### 6.8 InvestmentOfferController (550 lines)
**FXML**: Summary cards (Pending/Accepted/Rejected/Total), TableView, form
- **Action buttons per row**: Edit(✏️), Accept(✓, PENDING), Reject(✗, PENDING), Delete(🗑️)
- **`acceptOffer()`**: If Stripe configured → `PaymentService.createPaymentIntent()` → `offerService.acceptOffer()` + `markAsPaid()`
- **Investor combo**: Only loads `Role.INVESTOR` users
- **Opportunity combo**: OPEN opportunities for creation; all for editing

### 6.9 CoursController (430 lines)
**FXML**: Summary cards (TotalCours/CoursDebutant/CoursCertifiants/TotalXP), TableView<Cours>, form, filters
- **Table columns**: Id, Titre, Type (with icon), Niveau (with icon), PointsXP, Certification (✅/❌), Actions
- **Filters**: By NiveauCours, TypeCours, text search (titre + description)
- **Form fields**: titre, description, type (ComboBox), niveau (ComboBox), pointsXP, durée, certification (CheckBox)
- **Default form values**: `cboType=TEXTE`, `cboNiveau=DEBUTANT`, `pointsXP=100`, `durée=30`
- **Summary stats**: `compterTous()`, `compterParNiveau(DEBUTANT)`, `compterCertifiants()`, `calculerTotalXP()`

### 6.10 ApprentissageController (410 lines)
**FXML**: User stats (TotalXP, Niveau, CoursCompletes, Badges), ProgressBar for next level, TableView<Progression>, badges FlowPane, leaderboard TableView
- **User stats cards**: Total XP, current level, completed courses count, badges count
- **Progress bar**: Shows XP progress toward next level using `SEUILS_NIVEAU`
- **Progression table columns**: Cours, Pourcentage (with inline ProgressBar + color), XP, NiveauCours, Etat (with icon), Actions (▶️ Continue, 📊 Details)
  - ProgressBar colors: ≥100%=#27ae60, ≥50%=#f39c12, <50%=#3498db
- **Badge cards**: 100px wide, emoji icon (32px), name, "✅ Obtenu" or "🔒 Verrouillé", hover effect, tooltip with description
- **Leaderboard table**: Rang (with 🥇🥈🥉 medals), Nom, XP, Niveau — top 10
- **`checkNewBadges()`**: Calls `badgeService.verifierEtAttribuerBadges()`, shows congratulations popup
- **Filter**: By `EtatProgression` dropdown
- `currentUserId` defaults to 1 (TODO: replace with session)

### 6.11 EconomicDashboardController (475 lines)
**FXML**: Economic indicators, gauge, PieChart, BarChart, country selector, formula display, opportunity stats
- **Country selector**: 🇹🇳 Tunisie, 🇫🇷 France, 🇺🇸 États-Unis, 🇩🇪 Allemagne, 🇲🇦 Maroc, 🇪🇬 Égypte → country codes
- **Indicators**: EUR/USD, EUR/TND, GDP, Inflation (color-coded: >7%=red, >4%=orange, ≤4%=green)
- **Animated gauge**: Half-circle arc (230×135, radius=98, stroke=18), score arc animated 0→score over 1200ms, counter text animated simultaneously
- **PieChart**: Risk distribution of all opportunities (Faible/Moyen/Élevé/Non calculé), colors: #27ae60/#f39c12/#e74c3c/#95a5a6
- **BarChart**: Economic factors comparison (Change, PIB, Inflation, Composite) — bars colored by risk level
- **Formula display**: Shows scoring formula with actual computed values:
  - `Risk = (Montant × 0.3) + (Durée × 0.2) + (Éco × 0.5)`
  - `Éco = (Change × 0.3) + (PIB × 0.3) + (Inflation × 0.4)`
- **`analyzeAllOpportunities()`**: Batch-calculates risk for all opportunities using current economic data
- **Opportunity stats**: Total count, average risk score, high-risk count (>66)

### 6.12 FrontRiskAnalysisController (662 lines)
**FXML**: Economic indicators, interactive calculator, animated gauge, breakdown bars, opportunities list
- **Interactive calculator**: Amount (TextField, digits+decimal only), Deadline (DatePicker, default 6 months ahead), Country selector (5 countries)
- **Animated half-circle gauge**: 260×150, radius=110, stroke=22, with DropShadow effect
  - Score arc animated from 0 to score over 1500ms with counter
  - Gradient colors based on risk level
- **Breakdown progress bars**: Amount (weight=0.3), Duration (weight=0.2), Economic (weight=0.5) — each with label and ProgressBar
- **Pulse animation** on calculate button (800ms, scale 1.0→1.05, infinite)
- **AI Deep Analysis**: "🧠 Lancer l'Analyse Gemini" button appears after calculation
  - Calls `geminiService.analyzeRisk()` async
  - Displays response in styled TextFlow with **bold** parsing
- **Top 5 Opportunities**: Cards with colored risk dots, amount, deadline, staggered entrance (150ms per card)

### 6.13 FrontOpportunitiesController (555 lines)
**FXML**: Stats, search/filter, FlowPane cards, entrepreneur create form
- **Card layout**: 280px wide VBox, status badge (green/red/blue), project name, formatted amount, truncated description (100 chars), deadline badge
- **All cards have "🧠 Insights IA" button** → `showAIInsights()`:
  - Opens centered popup overlay with `GaussianBlur(8)` on background
  - Gradient header, loading spinner
  - Calls `geminiService.generateProjectInsights()` async
  - Renders AI response in ScrollPane with bold text parsing
  - Animated scale+fade popup entrance (250ms)
  - Close button + blur removal
- **Create form** (entrepreneur only): Project dropdown, amount, description, deadline. Validates amount>0, description required, deadline not past.
- **Navigation**: "Investir" button → `FrontOfficeController.showOffers(opportunityId)`

### 6.14 FrontOffersController (1260 lines — largest controller)
**FXML**: Stats (with animated counters), create form, filter, FlowPane cards
- **Card design**: Colored top stripe (PENDING=#f39c12, ACCEPTED=#27ae60, REJECTED=#e74c3c), status badge, amount, investor name, opportunity snippet, action buttons
  - PENDING: 📄 Détails + ❌ Annuler
  - ACCEPTED (unpaid): 💳 Payer & Détails
  - ACCEPTED (paid): ✅ Payé badge + 📋 Détails
  - REJECTED: 📄 Voir Détails
- **Hover effect**: Colored drop shadow + scale 1.03 animation
- **Detail popup** (all statuses): Full-screen overlay with GaussianBlur(6), centered VBox (540×720)
  - **Header**: Gradient based on status, pulsing icon, shimmer animation on amount
  - **Body sections**: Offer details, Opportunity info (with risk bar if scored), deadline badge, Project info
  - **Footer varies by status**:
    - PENDING: Animated "En attente de validation..." with spinner
    - REJECTED: "Cette offre a été déclinée" message
    - ACCEPTED: Stripe payment flow (see below)
  - **Animations**: iOS-like spring interpolator `SPLINE(0.16, 1.0, 0.3, 1.0)`, 450ms open, 250ms close
- **Stripe Payment Flow** (ACCEPTED offers):
  1. "💳 Payer via Stripe" button with glowing DropShadow animation
  2. Card entry form appears (350ms fade+slide):
     - Card Number: 16-digit auto-format with spaces, **Luhn algorithm** validation
     - Expiry: MM/YY auto-format, future date check
     - CVV: 3-4 digits
     - Cardholder Name: required, ≥2 chars
     - Field focus highlights with Stripe purple (#6772e5)
  3. Validation errors shown per field with red border
  4. On submit: `PaymentService.createPaymentIntent()` async
  5. Success: 🎊 confetti animation (scale 0.3→1.2, fade), "Paiement réussi!", shows PaymentIntent ID
  6. Auto-redirect to Portfolio after 2.5s pause
  7. Failure: "Réessayer" button
- **Offer submission**: Validates opportunity selected, amount > 0, investor logged in
- **Pre-selection**: `preselectOpportunity(int id)` opens form with opportunity selected
- **Paid offers excluded**: `loadData()` filters `!offer.isPaid()` (paid offers go to Portfolio)
- **Navigation**: After payment success → `FrontOfficeController.showPortfolio()`

### 6.15 FrontPortfolioController (479 lines)
**FXML**: Stats (TotalInvested, TotalCount, AvgAmount), FlowPane cards, emptyState
- **Shows only paid offers**: `offerService.findAll().stream().filter(InvestmentOffer::isPaid)`
- **Stats**: Total invested (formatted €), count, average amount
- **Card design**: Green gradient header (#27ae60→#2ecc71), ✅ icon, amount, "PAYÉ" badge, project title, sector tag (purple), opportunity snippet, payment info (PaymentIntent ID, paid date)
- **Hover effect**: Green drop shadow (#27ae6055) + scale 1.02
- **Detail popup**: Green gradient header, "Investissement Confirmé", sections for Offer/Opportunity/Project/Payment details
  - Payment section: Transaction ID, paid date, "Stripe (paiement sécurisé)", secure notice about data handling
  - "Investissement confirmé et sécurisé" confirmation badge

### 6.16 AIChatWidget (508 lines — programmatic, no FXML)
**Singleton per host-pane** via `AIChatWidget.attachTo(StackPane hostPane)`
- **Toggle button**: 60×60 red circle (#e94560), 🤖 icon, floating bounce animation (2000ms), bottom-right corner
- **Chat panel**: 380×520, scale+fade animation on open (250ms) / close (200ms)
- **Header**: Shows active backend: "🟢 [HF model name]" or "☁️ Gemini Cloud"
- **Quick actions**: 3 buttons with predefined prompts:
  - "💰 Conseils" → investment advice prompt
  - "📊 Risques" → risk analysis prompt
  - "🇹🇳 Marché TN" → Tunisian market prompt
- **Message UI**: User bubbles right-aligned (white on colored), Bot bubbles left-aligned with 🤖 avatar
- **Typing indicator**: 3 bouncing dots with staggered animation (200/400/600ms delays)
- **Markdown parsing**: Detects `**bold**` text, handles newlines
- **Welcome message**: Personalized greeting with user name, lists capabilities
- **Clear history**: Resets GeminiService conversation and clears messages
- Uses `GeminiService.chat(text)` returning CompletableFuture

---

## 7. UTILITY CLASSES

### 7.1 DBConnection
**File**: `com.najahni.utils.DBConnection`
- Singleton JDBC connection
- `URL`: `jdbc:mysql://localhost:3306/najahni_db`
- `USER`: `root`, `PASSWORD`: `` (empty)
- `getConnection()` → Connection (creates if null or closed)

### 7.2 AlertUtils
**File**: `com.najahni.utils.AlertUtils`
- `showInfo(String title, String message)` — INFORMATION alert
- `showSuccess(String message)` — wraps showInfo with "Succès"
- `showError(String title, String message)` — ERROR alert
- `showValidationError(String message)` — wraps showError with "Erreur de Validation"
- `showWarning(String title, String message)` — WARNING alert
- `showConfirmation(String title, String message)` → boolean
- `confirmDelete(String itemName)` → boolean — "Confirmer la Suppression" with irreversibility warning

### 7.3 AnimationUtils (380 lines)
**File**: `com.najahni.utils.AnimationUtils`
- Private constructor (non-instantiable utility)

**Basic Animations**:
- `fadeIn(Node, int durationMs)` → FadeTransition (0→1)
- `fadeOut(Node, int durationMs)` → FadeTransition (1→0)
- `slideInFromBottom(Node, int durationMs, double distance)` → TranslateTransition
- `slideInFromRight(Node, int durationMs, double distance)` → TranslateTransition
- `scale(Node, int durationMs, double from, double to)` → ScaleTransition

**Combined Animations**:
- `playFadeSlideIn(Node, int durationMs)` — fade + slide from 20px below
- `playFadeScaleIn(Node, int durationMs, int delayMs)` — fade + scale from 0.95 (most used)

**Interactive Feedback**:
- `pulse(Node)` → ScaleTransition (1.0→1.05, 2 cycles)
- `playSuccessFeedback(Node)` — subtle scale 1.0→1.02
- `playButtonPress(Node)` — shrink 0.95 then release 1.0
- `animateTableRefresh(TableView)` — fade 0.7→1.0

**Staggered Animations**:
- `playStaggeredFadeIn(Node[], int delayBetweenMs)` — sequential with delays
- `playStaggeredCardEntrance(Parent container, int staggerMs)` — slide up + fade per child

**Premium Animations v3.0**:
- `playElasticBounceIn(Node, int durationMs, int delayMs)` — elastic overshoot (0.3→1.08→0.96→1.0)
- `playSlideInFromLeft(Node, int durationMs, int delayMs)` — slide + fade with spline interpolator
- `floatAnimation(Node)` → TranslateTransition (continuous 0→-6px bounce, infinite)
- `playShimmer(Node)` — fade 0.7→1.0, 4 cycles
- `playRipplePress(Node)` — shrink 0.92 → expand 1.04 → settle 1.0
- `playTypewriterReveal(Node, int durationMs)` — fade + scale 0.97→1.0
- `playShake(Node)` — horizontal shake ±8px, 6 cycles (error feedback)
- `playCountPop(Node)` — scale 0.6→1.12→1.0 (stat number animation)

### 7.4 WrappedTextCellFactory
**File**: `com.najahni.utils.WrappedTextCellFactory<S>`
- Implements `Callback<TableColumn<S, String>, TableCell<S, String>>`
- Prevents text truncation in TableView columns
- Uses `javafx.scene.text.Text` with `wrappingWidthProperty` bound to column width - 15px
- Text color: `#2c3e50`
- Constructors: default (LEFT alignment) or `WrappedTextCellFactory(TextAlignment, boolean centerVertically)`
- Static factory: `centered()` → center-aligned variant

---

## 8. DATABASE SCHEMA

### 8.1 Core Tables

```sql
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `firstname` varchar(50) NOT NULL,
  `lastname` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB;

CREATE TABLE `projet` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `sector` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `entrepreneur_id` int NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM;  -- NOTE: MyISAM, no FK support

CREATE TABLE `investment_opportunity` (
  `id` int NOT NULL AUTO_INCREMENT,
  `target_amount` decimal(15,2) NOT NULL,
  `description` text,
  `deadline` date DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `project_id` int NOT NULL,
  `risk_score` double DEFAULT NULL,
  `risk_label` varchar(100) DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_opp_status` (`status`),
  KEY `idx_opp_project` (`project_id`),
  KEY `idx_opp_deadline` (`deadline`)
) ENGINE=InnoDB;

CREATE TABLE `investment_offer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `proposed_amount` decimal(15,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `investor_id` int NOT NULL,
  `opportunity_id` int NOT NULL,
  `paid` tinyint(1) DEFAULT '0',
  `payment_intent_id` varchar(255) DEFAULT NULL,
  `paid_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_offer_investor` (`investor_id`),
  KEY `fk_offer_opportunity` (`opportunity_id`),
  KEY `idx_offer_status` (`status`),
  FOREIGN KEY (`investor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`opportunity_id`) REFERENCES `investment_opportunity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
```

### 8.2 Learning Tables

```sql
CREATE TABLE `cours` (
  `id` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(200) NOT NULL,
  `description` text,
  `type` varchar(20) NOT NULL,
  `niveau` varchar(20) NOT NULL,
  `certification` tinyint(1) DEFAULT '0',
  `points_xp` int DEFAULT '100',
  `duree_minutes` int DEFAULT '30',
  `image_url` varchar(500) DEFAULT NULL,
  `createur_id` int DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cours_categorie` (`type`),
  KEY `idx_cours_niveau` (`niveau`),
  KEY `idx_cours_actif` (`certification`)
) ENGINE=InnoDB;

CREATE TABLE `progression` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `cours_id` int NOT NULL,
  `pourcentage` double DEFAULT '0',
  `points_xp` int DEFAULT '0',
  `niveau` int DEFAULT '1',
  `etat` varchar(20) DEFAULT 'NON_COMMENCE',
  `date_debut` timestamp DEFAULT CURRENT_TIMESTAMP,
  `date_obtention` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_cours` (`user_id`, `cours_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`cours_id`) REFERENCES `cours` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `badge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `description` text,
  `icone` varchar(50) DEFAULT NULL,
  `condition_type` varchar(50) DEFAULT NULL,
  `points_requis` int DEFAULT '0',
  `cours_requis` int DEFAULT '0',
  `niveau_requis` int DEFAULT '0',
  `categorie` varchar(50) DEFAULT NULL,
  `rarete` varchar(30) DEFAULT 'COMMUN',
  `actif` tinyint(1) DEFAULT '1',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nom` (`nom`),
  KEY `idx_badge_rarete` (`rarete`),
  KEY `idx_badge_actif` (`actif`)
) ENGINE=InnoDB;
```

### 8.3 Social/Forum Tables (in DB but not used by this JavaFX app)

- `threads` — forum threads in groups
- `comments` — thread comments
- `groups` — user groups with admin
- `group_members` — group membership (UNIQUE group_id+user_id)
- `group_join_request` — pending join requests
- `posts` — user posts
- `post_reactions` — reactions (UNIQUE post_id+user_id)
- `user_connection` — follower/following (UNIQUE follower_id+followed_id)
- `events` — community events
- `event_participants` — event signups (UNIQUE event_id+user_id)
- `demande_mentorat` — mentorship requests
- `session_mentorat` — mentorship sessions (FK → demande_mentorat)
- `donnees_business` — business data (FK → projet)

All use InnoDB with CASCADE ON DELETE/UPDATE.

---

## 9. RISK SCORING SYSTEM

### 9.1 Simple Risk (RiskCalculator + RiskService)

```
risk = (amountFactor × 0.4) + (durationFactor × 0.2) + (weatherApiFactor × 0.4)
```

**Amount Factor**: `min(amount / 500000 × 100, 100)`

**Duration Factor** (days until deadline):
| Condition | Factor |
|---|---|
| Expired (< 0 days) | 100 |
| ≤ 30 days | 20 |
| ≤ 90 days | 40 |
| ≤ 180 days | 60 |
| ≤ 365 days | 80 |
| > 365 days | 90 |

**Weather API Factor** (Open-Meteo, Tunis coordinates):
- Temperature: 15-25°C=20, 5-15°C=40, 25-35°C=45, <5°C=70, >35°C=75
- Wind: ≤10=10, 10-25=25, 25-40=50, 40-60=75, >60=95
- Factor = `temp × 0.7 + wind × 0.3`

### 9.2 Enhanced Risk (EconomicRiskEngine)

```
risk = (amountFactor × 0.3) + (durationFactor × 0.2) + (economicFactor × 0.5)

economicFactor = (exchangeScore × 0.3) + (gdpScore × 0.3) + (inflationScore × 0.4)
```

**Normalization**:
- Exchange rate: EUR/USD > 1.20 = high risk, < 1.05 = low risk
- GDP: > 1000B = low risk, < 10B = high risk
- Inflation: > 10% = very high risk, < 2% = low risk

**Risk Levels & Colors**:
| Score | Level | Color | Emoji |
|---|---|---|---|
| 0-15 | Très Faible | #27ae60 | 🟢 |
| 16-33 | Faible | #2ecc71 | 🟢 |
| 34-50 | Modéré | #f39c12 | 🟡 |
| 51-66 | Significatif | #e67e22 | 🟠 |
| 67-85 | Élevé | #e74c3c | 🔴 |
| 86-100 | Critique | #c0392b | ⛔ |

---

## 10. EXTERNAL API INTEGRATIONS

| API | URL | Purpose | Auth |
|---|---|---|---|
| Open Exchange Rates | `https://open.er-api.com/v6/latest/EUR` | Currency exchange rates | None (free) |
| World Bank (GDP) | `https://api.worldbank.org/v2/country/{code}/indicator/NY.GDP.MKTP.CD` | GDP data | None |
| World Bank (Inflation) | `https://api.worldbank.org/v2/country/{code}/indicator/FP.CPI.TOTL.ZG` | Inflation data | None |
| Open-Meteo | `https://api.open-meteo.com/v1/forecast?latitude=36.80&longitude=10.18&current_weather=true` | Weather (Tunis) | None |
| HuggingFace | `https://router.huggingface.co/v1/chat/completions` | AI chat (Llama 3.2) | Bearer token |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | AI fallback | API key |
| Stripe | `https://api.stripe.com/v1/payment_intents` | Payments | Secret key |

---

## 11. CSS ARCHITECTURE

**Single stylesheet**: `/css/styles.css` (2544 lines)
**Global font**: Segoe UI, Arial, sans-serif (14px)
**Background**: #f0f2f5

### Key CSS Class Families

**BackOffice**:
- `.header` — gradient: #1a1a2e → #16213e → #0f3460
- `.nav-menu` — #34495e sidebar
- `.nav-button` — sidebar navigation items
- `.btn-primary` — primary action buttons
- `.btn-success` / `.btn-danger` / `.btn-warning` / `.btn-secondary`
- `.stat-card` — dashboard statistic cards

**FrontOffice** (`.front-*`):
- `.front-page`, `.front-scroll` — page containers
- `.front-hero`, `.front-hero-title`, `.front-hero-subtitle` — hero section
- `.front-stat`, `.front-stat-value` (+ -green, -blue, -purple), `.front-stat-label` — stat cards
- `.front-card` — opportunity/offer cards with hover effect
- `.front-card-project`, `.front-card-amount`, `.front-card-desc`, `.front-card-meta`
- `.front-badge` (+ -green, -red, -blue, -orange) — status badges
- `.front-btn-primary`, `.front-btn-secondary`, `.front-btn-invest`, `.front-btn-danger`
- `.front-titled-pane`, `.front-form-inner`, `.front-form-pane` — form styling
- `.front-filter-bar`, `.front-search-field`, `.front-combo` — filters

**Payment Popup** (`.payment-*`):
- `.payment-overlay` — semi-transparent backdrop
- `.payment-popup` — centered card with rounded corners
- `.payment-popup-header`, `.payment-popup-body`, `.payment-popup-footer`
- `.payment-close-btn` — close button
- `.payment-scroll` — custom scrollbar styling
- `.payment-section`, `.payment-section-title`
- `.payment-detail-key`, `.payment-detail-value`
- `.payment-btn-stripe` — Stripe-branded button
- `.payment-btn-cancel` — cancel/close button

**Risk Analysis**:
- `.btn-calculate` — calculate risk button
- `.btn-pulse` — pulsing animation button

---

## 12. DESIGN PATTERNS & CONVENTIONS

### Patterns Used
1. **Singleton**: `SessionManager`, `DBConnection`, `AIChatWidget` (per host-pane)
2. **MVC**: Controllers + FXML Views + Model classes + Service layer
3. **DAO pattern**: Services encapsulate JDBC operations
4. **Strategy**: Dual AI backend (HuggingFace primary, Gemini fallback)
5. **Observer**: FXML property bindings, ComboBox listeners

### Naming Conventions
- **Controllers**: PascalCase + `Controller` suffix
- **Services**: PascalCase + `Service` suffix
- **Models**: PascalCase (entity name)
- **FXML files**: PascalCase + `View.fxml` suffix
- **French naming** in learning module: `creerCours`, `trouverParId`, `compterTous`, `demarrerCours`, `mettreAJourPourcentage`, `completerCours`
- **FXML IDs**: camelCase with prefixes: `lbl` (Label), `txt` (TextField), `cbo` (ComboBox), `dp` (DatePicker), `btn` (Button), `col` (TableColumn), `pb` (ProgressBar), `fo` (FrontOffice prefix)

### Data Flow
1. User interacts with FXML view
2. Controller handles via `@FXML` methods
3. Controller calls Service
4. Service uses JDBC via `DBConnection.getConnection()`
5. Service returns model objects
6. Controller updates UI (often via `Platform.runLater()` for async operations)

### Async Patterns
- `CompletableFuture` for API calls (GeminiService, PaymentService, CurrencyService, EconomicApiService)
- `CompletableFuture.supplyAsync()` + `.thenAccept(result -> Platform.runLater(...))` for UI updates
- All external API calls are non-blocking

### Error Handling
- Service-level: try-catch with `System.err.println()` logging
- Controller-level: `AlertUtils.showError()` dialogs
- API-level: Fallback values when APIs fail (CurrencyService fallback rates, EconomicApiService fallback GDP/inflation)
- Payment: `PaymentResult.isSuccess()` check with error message display

---

## 13. KEY BUSINESS RULES

1. **Only entrepreneurs** can create projects (`ProjectService` validates entrepreneur role)
2. **Only APPROVED projects** appear for investment (`getProjectsForInvestment()`)
3. **Only OPEN opportunities** are available for new offers
4. **Offer acceptance** triggers Stripe payment flow
5. **Paid offers** move from "Offres" view to "Portfolio" view
6. **Entrepreneurs see** offers on their projects in the Inbox panel
7. **Risk scoring** can use either simple (weather-based) or enhanced (economic-based) formulas
8. **Course completion** awards XP and triggers automatic badge checking
9. **Progression** is unique per user + course combination (DB UNIQUE constraint)
10. **Auto-migration**: `InvestmentOfferService` automatically creates `paid`, `payment_intent_id`, `paid_at` columns if they don't exist

---

## 14. SCENE DIMENSIONS & STYLING

| View | Width | Height | Title |
|---|---|---|---|
| Login | 900 | 600 | "NAJAHNI — Connexion" |
| BackOffice | 1200 | 700 | "Back-Office" |
| FrontOffice (Investor) | 1200 | 700 | "Espace Investisseur" |
| FrontOffice (Entrepreneur) | 1200 | 700 | "Espace Entrepreneur" |

**Minimum window**: 800×550
**Transition animations**: fade-in 300-400ms on view load
**Card animations**: staggered entrance with 70-150ms delay between cards
**Popup animations**: scale 0.85→1.0 + fade + slide, spring interpolator

---

*Last updated: Knowledge base generated from complete source analysis of the NAJAHNI application.*
