# 🤖 Module Investment Risk AI — Machine Learning

## Vue d'ensemble

Module de prédiction du risque d'investissement basé sur le **Machine Learning**,
utilisant l'algorithme **RandomForest** de la bibliothèque [Weka 3.8.6](https://www.cs.waikato.ac.nz/ml/weka/).

### Caractéristiques

| Propriété          | Valeur                                     |
|--------------------|--------------------------------------------|
| **Algorithme**     | RandomForest (ensemble d'arbres de décision) |
| **Bibliothèque**   | Weka 3.8.6 (`nz.ac.waikato.cms.weka:weka-stable`) |
| **Arbres**         | 100 (bagging)                              |
| **Seed**           | 42 (reproductibilité garantie)             |
| **Classes**        | 3 : `faible`, `moyen`, `eleve`             |
| **Features**       | 5 : montant, durée, secteur, success_rate, funding_ratio |
| **Dataset**        | 60 instances (20 par classe)               |
| **API externe**    | ❌ Aucune — 100% local                     |

---

## Architecture

```
com.najahni.services.ml/
├── RiskModelTrainer.java    # Entraînement et sauvegarde du modèle
├── RiskPredictor.java       # Chargement modèle + prédiction
├── RiskPrediction.java      # DTO résultat (label + probabilités)
└── RiskAIService.java       # Service d'intégration (Controller → ML)

resources/ml/
├── investment_risk_dataset.arff    # Dataset d'entraînement (ARFF)
└── investment_risk.model           # Modèle sérialisé (généré)
```

### Diagramme de séquence

```
┌──────────┐     ┌──────────────┐     ┌───────────────┐     ┌──────────────┐
│Controller│     │ RiskAIService│     │ RiskPredictor  │     │  RandomForest│
└────┬─────┘     └──────┬───────┘     └───────┬────────┘     └──────┬───────┘
     │                  │                     │                     │
     │ predictRiskML()  │                     │                     │
     │─────────────────>│                     │                     │
     │                  │                     │                     │
     │                  │ initialize()        │                     │
     │                  │ (auto-train si      │                     │
     │                  │  modèle absent)     │                     │
     │                  │                     │                     │
     │                  │ predictRisk(opp)    │                     │
     │                  │─────────────────────>                     │
     │                  │                     │                     │
     │                  │                     │ predict(features)   │
     │                  │                     │ ─ createInstance()  │
     │                  │                     │────────────────────>│
     │                  │                     │   classifyInstance()│
     │                  │                     │<────────────────────│
     │                  │                     │   distribution()    │
     │                  │                     │<────────────────────│
     │                  │                     │                     │
     │                  │  RiskPrediction     │                     │
     │                  │<─────────────────────                     │
     │                  │                     │                     │
     │ RiskPrediction   │                     │                     │
     │<─────────────────│                     │                     │
     │                  │                     │                     │
     │ updateRiskLabel()│                     │                     │
     │────────> BDD     │                     │                     │
     │                  │                     │                     │
     │ showPopup()      │                     │                     │
     │                  │                     │                     │
```

---

## Dataset ARFF

### Format

```arff
@relation investment_risk

@attribute montant numeric
@attribute duree numeric
@attribute secteur {tech,immobilier,sante,industrie}
@attribute success_rate numeric
@attribute funding_ratio numeric
@attribute risk_label {faible,moyen,eleve}

@data
3000,500,tech,85,0.92,faible
200000,30,industrie,10,0.05,eleve
...
```

### Distribution des classes

| Classe    | Instances | Caractéristiques typiques |
|-----------|-----------|--------------------------|
| `faible`  | 20        | Petits montants, longues durées, bons indicateurs |
| `moyen`   | 20        | Valeurs intermédiaires |
| `eleve`   | 20        | Gros montants, courtes durées, mauvais indicateurs |

### Attributs (Features)

| Attribut       | Type     | Plage          | Description |
|----------------|----------|----------------|-------------|
| `montant`      | numeric  | 1 000 – 250 000 | Montant cible de l'investissement (€) |
| `duree`        | numeric  | 15 – 600       | Durée restante avant la deadline (jours) |
| `secteur`      | nominal  | {tech, immobilier, sante, industrie} | Secteur d'activité du projet |
| `success_rate` | numeric  | 5 – 95         | Taux de succès historique du projet (%) |
| `funding_ratio`| numeric  | 0.02 – 0.98    | Ratio montant obtenu / montant cible |
| `risk_label`   | nominal  | {faible, moyen, eleve} | **Classe cible** |

---

## Algorithme : RandomForest

### Principe

RandomForest est un algorithme d'**ensemble learning** (bagging) qui construit
plusieurs arbres de décision indépendants et agrège leurs votes :

$$
\hat{y} = \text{mode}\big(h_1(x), h_2(x), \ldots, h_{100}(x)\big)
$$

Chaque arbre $h_i$ est entraîné sur un sous-échantillon bootstrap du dataset,
avec une sélection aléatoire de features à chaque nœud.

### Avantages pour ce cas d'usage

1. **Robuste au surapprentissage** : Le bagging réduit la variance
2. **Gère les features mixtes** : Numériques (montant, durée) + nominales (secteur)
3. **Distribution de probabilités** : Retourne les probabilités pour les 3 classes
4. **Déterministe** : Avec `seed=42`, les résultats sont parfaitement reproductibles
5. **Pas de normalisation requise** : Contrairement aux SVM ou réseaux de neurones

### Configuration Weka

```java
RandomForest rf = new RandomForest();
rf.setNumIterations(100);  // 100 arbres
rf.setSeed(42);            // Reproductibilité
rf.buildClassifier(dataset);
```

### Interprétation des résultats

La prédiction retourne :
- **Label** : `"faible"`, `"moyen"`, ou `"eleve"`
- **Probabilité** : Confiance de la prédiction (0.0 à 1.0)
- **Distribution** : Probabilités pour chaque classe

Exemple :
```
Prédiction : MOYEN (confiance : 72.3%)
Distribution : faible=15.2% | moyen=72.3% | eleve=12.5%
```

---

## Intégration dans l'application

### Bouton UI

Le bouton **🤖** dans la colonne Actions lance la prédiction ML.
Il est distinct du bouton **🎯** (Risk Score API Open-Meteo).

### Flux de données

1. L'utilisateur clique sur **🤖** pour une opportunité
2. Le contrôleur récupère le secteur du projet associé
3. `RiskAIService` extrait les features de l'`InvestmentOpportunity`
4. `RiskPredictor` crée une Instance Weka et prédit
5. Le résultat est affiché dans un popup détaillé
6. Le `risk_label` est sauvegardé en BDD
7. La colonne **🤖 ML** du tableau est mise à jour

### Extraction des features

| Feature        | Source                          | Défaut |
|----------------|---------------------------------|--------|
| `montant`      | `opportunity.getTargetAmount()` | 10 000 |
| `duree`        | Jours entre aujourd'hui et deadline | 180 |
| `secteur`      | `project.getSector()` (normalisé) | tech |
| `success_rate` | Basé sur le statut du projet    | 50     |
| `funding_ratio`| Basé sur le statut du projet    | 0.2    |

### Normalisation des secteurs

Le mapping est flexible et gère les accents, majuscules et variantes :

| Entrées acceptées | Valeur ARFF |
|-------------------|-------------|
| tech, Technology, Technologie, informatique, digital, IA | `tech` |
| immobilier, Real Estate, construction, bâtiment | `immobilier` |
| santé, Health, médical, pharma, bio | `sante` |
| industrie, Manufacturing, usine, énergie | `industrie` |

---

## Tests unitaires

**41 tests** organisés en 7 catégories :

| Catégorie | Tests | Description |
|-----------|-------|-------------|
| Dataset Loading | 6 | Chargement ARFF, taille, attributs, classes |
| Model Training | 4 | Entraînement RF, type, classification, distribution |
| Prediction | 5 | Prédiction valide, labels, probabilités |
| Consistency | 4 | Déterminisme, secteurs, risque faible/élevé |
| RiskAIService | 2 | Intégration service, secteurs inconnus |
| Sector Normalization | 12 | Mapping de tous les secteurs |
| DTO Display | 5 | Affichage, emojis, pourcentages |

Exécution :
```bash
mvn test -Dtest=com.najahni.services.ml.RiskMLTest
```

---

## Schema SQL

```sql
ALTER TABLE investment_opportunity 
ADD COLUMN risk_label VARCHAR(20) DEFAULT NULL 
COMMENT 'Label ML: faible, moyen, eleve';
```

---

## Références académiques

- **Breiman, L. (2001)**. "Random Forests". *Machine Learning*, 45(1), 5-32.
- **Witten, I. H., Frank, E., & Hall, M. A. (2011)**. *Data Mining: Practical Machine Learning Tools and Techniques*. Morgan Kaufmann.
- **Weka Documentation** : https://weka.sourceforge.io/doc.stable/
