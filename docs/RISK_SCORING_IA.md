# 🎯 Module Risk Scoring IA — Documentation

## Architecture du Module

```
┌─────────────────────────────────────────────────────────┐
│                    Couche UI (JavaFX)                    │
│                                                         │
│  InvestmentOpportunityController                        │
│  ├── Bouton "🎯 Calculer Risque" dans chaque ligne     │
│  ├── Popup de confirmation avant calcul                 │
│  ├── Colonne "Risque IA" avec score coloré              │
│  └── Popup résultat avec score + niveau                 │
│                                                         │
│  InvestmentView.fxml                                    │
│  └── TableColumn "🎯 Risque IA" (colRiskScore)         │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Couche Service                          │
│                                                         │
│  RiskService                                            │
│  ├── calculateRisk(opportunity) → RiskResult            │
│  ├── fetchApiFactor() → appel HTTP Open-Meteo           │
│  └── extractJsonDouble() → parsing JSON manuel          │
│                                                         │
│  RiskCalculator (logique pure)                          │
│  ├── computeScore(montant, deadline, apiFactor)         │
│  ├── computeAmountFactor(montant) → 0–100               │
│  ├── computeDurationFactor(deadline) → 0–100            │
│  └── getRiskLevel(score) → Faible/Moyen/Élevé          │
│                                                         │
│  RiskResult (DTO immuable)                              │
│  ├── score (int 0–100)                                  │
│  ├── level (String)                                     │
│  └── emoji (String)                                     │
│                                                         │
│  InvestmentOpportunityService                           │
│  └── updateRiskScore(id, score) → UPDATE SQL            │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│               Couche Données                            │
│                                                         │
│  MySQL (najahni_db)                                     │
│  └── investment_opportunity                             │
│      └── risk_score DOUBLE DEFAULT NULL                 │
│                                                         │
│  API Externe                                            │
│  └── Open-Meteo (gratuite, sans token)                  │
│      └── GET /v1/forecast?latitude=36.80&longitude=...  │
└─────────────────────────────────────────────────────────┘
```

## Diagramme de Séquence Simplifié

```
Utilisateur          Controller               RiskService            RiskCalculator         API Open-Meteo         BDD (MySQL)
    │                     │                        │                       │                      │                    │
    │  Clic "🎯"         │                        │                       │                      │                    │
    │────────────────────►│                        │                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │  Popup Confirmation │                        │                       │                      │                    │
    │◄────────────────────│                        │                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │  "Oui, calculer"   │                        │                       │                      │                    │
    │────────────────────►│                        │                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │                     │  calculateRisk(opp)    │                       │                      │                    │
    │                     │───────────────────────►│                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │                     │                        │  HTTP GET /forecast   │                      │                    │
    │                     │                        │─────────────────────────────────────────────►│                    │
    │                     │                        │                       │                      │                    │
    │                     │                        │  JSON (temp, vent)    │                      │                    │
    │                     │                        │◄─────────────────────────────────────────────│                    │
    │                     │                        │                       │                      │                    │
    │                     │                        │  computeScore(        │                      │                    │
    │                     │                        │    montant, deadline, │                      │                    │
    │                     │                        │    apiFactor)         │                      │                    │
    │                     │                        │──────────────────────►│                      │                    │
    │                     │                        │                       │                      │                    │
    │                     │                        │  score (0–100)        │                      │                    │
    │                     │                        │◄──────────────────────│                      │                    │
    │                     │                        │                       │                      │                    │
    │                     │  RiskResult            │                       │                      │                    │
    │                     │◄───────────────────────│                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │                     │  updateRiskScore(id, score)                    │                      │                    │
    │                     │──────────────────────────────────────────────────────────────────────────────────────────►│
    │                     │                        │                       │                      │                    │
    │                     │  UPDATE OK             │                       │                      │                    │
    │                     │◄──────────────────────────────────────────────────────────────────────────────────────────│
    │                     │                        │                       │                      │                    │
    │  Popup Résultat     │                        │                       │                      │                    │
    │  "🟢 25/100 Faible" │                        │                       │                      │                    │
    │◄────────────────────│                        │                       │                      │                    │
    │                     │                        │                       │                      │                    │
    │  Refresh TableView  │                        │                       │                      │                    │
    │◄────────────────────│                        │                       │                      │                    │
```

## Formule de Calcul

$$\text{risk\_score} = (\text{facteurMontant} \times 0.4) + (\text{facteurDurée} \times 0.2) + (\text{facteurAPI} \times 0.4)$$

### Facteur Montant (0–100)
| Montant | Facteur |
|---------|---------|
| 0 € | 0 |
| 100 000 € | 20 |
| 250 000 € | 50 |
| 500 000 € | 100 |
| > 500 000 € | 100 (plafonné) |

### Facteur Durée (0–100)
| Jours restants | Facteur | Signification |
|---------------|---------|---------------|
| ≤ 0 (passé) | 100 | Risque maximum |
| ≤ 7 | 90 | Très court terme |
| ≤ 30 | 70 | Court terme |
| ≤ 90 | 50 | Moyen terme |
| ≤ 180 | 30 | Semi-long terme |
| ≤ 365 | 15 | Long terme |
| > 365 | 10 | Très long terme |

### Facteur API (0–100)
Basé sur les données météorologiques de **Open-Meteo** (Tunis, Tunisie) :
- **Température** (70% du facteur) : Extrêmes augmentent le risque
- **Vent** (30% du facteur) : Vents forts augmentent le risque

### Niveaux de Risque
| Score | Niveau | Emoji | Couleur |
|-------|--------|-------|---------|
| 0 – 33 | Faible | 🟢 | Vert (#27ae60) |
| 34 – 66 | Moyen | 🟡 | Orange (#f39c12) |
| 67 – 100 | Élevé | 🔴 | Rouge (#e74c3c) |

## Fichiers Créés / Modifiés

### Nouveaux Fichiers
| Fichier | Description |
|---------|-------------|
| `RiskCalculator.java` | Logique pure de calcul du score (aucune dépendance externe) |
| `RiskService.java` | Orchestrateur : API + calcul + retour résultat |
| `RiskResult.java` | DTO immuable contenant score + niveau + emoji |
| `RiskCalculatorTest.java` | 31 tests JUnit couvrant tous les scénarios |

### Fichiers Modifiés
| Fichier | Modification |
|---------|-------------|
| `InvestmentOpportunity.java` | Ajout champ `riskScore` + `getRiskLevel()` + `getFormattedRiskScore()` |
| `InvestmentOpportunityService.java` | `risk_score` dans INSERT/UPDATE/SELECT + `updateRiskScore()` |
| `InvestmentOpportunityController.java` | Colonne `colRiskScore` + bouton 🎯 + méthode `calculateRisk()` |
| `InvestmentView.fxml` | Colonne `🎯 Risque IA` dans le TableView |
| `module-info.java` | Ajout `requires java.net.http;` |
| `schema_investment.sql` | Colonne `risk_score DOUBLE DEFAULT NULL` |

## SQL Migration

```sql
-- Si la table existe déjà, exécuter :
ALTER TABLE investment_opportunity ADD COLUMN risk_score DOUBLE DEFAULT NULL COMMENT 'Score de risque IA (0-100)';
```

## Tests Unitaires (47 tests — 0 échecs)

| Classe de Test | Nb Tests | Description |
|---------------|----------|-------------|
| `RiskCalculatorTest.ComputeScoreTests` | 10 | Score global (faible/moyen/élevé, limites, exceptions) |
| `RiskCalculatorTest.AmountFactorTests` | 4 | Normalisation du montant |
| `RiskCalculatorTest.DurationFactorTests` | 7 | Normalisation de la durée |
| `RiskCalculatorTest.RiskLevelTests` | 6 | Classification Faible/Moyen/Élevé |
| `RiskCalculatorTest.JsonParsingTests` | 4 | Parsing JSON Open-Meteo |
| `RiskCalculatorTest.VisualIndicatorsTests` | 3 | Emojis et styles CSS |
| `InvestmentOpportunityServiceTest` | 6 | CRUD existants (montant, deadline, fermeture) |
| Autres tests existants | 7 | Tests ProjectService, CoursService, etc. |
