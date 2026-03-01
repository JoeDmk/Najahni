# 🎬 Scénario de Démonstration — Module Investissement

## Contexte du scénario

> **Personnage** : Ahmed, un investisseur expérimenté, utilise NAJAHNI pour trouver des projets prometteurs,
> analyser les risques, investir, et suivre son portefeuille.

---

## 📝 Données de test à préparer

### Utilisateurs

| # | Nom | Email | Rôle | Mot de passe |
|---|-----|-------|------|-------------|
| 1 | Ahmed Investisseur | investor@test.com | INVESTOR | test123 |
| 2 | Sara Entrepreneur | entrepreneur@test.com | ENTREPRENEUR | test123 |
| 3 | Admin NAJAHNI | admin@najahni.com | ADMIN | admin123 |

### Projets (créés par Sara)

| # | Titre | Secteur | Budget | Statut |
|---|-------|---------|--------|--------|
| 1 | EcoTech Solutions | Technologie | 150 000 € | APPROVED |
| 2 | Green Energy Maroc | Énergie | 250 000 € | APPROVED |
| 3 | FoodHub Delivery | Restauration | 80 000 € | APPROVED |

### Opportunités d'investissement

| # | Projet | Montant Min | Durée (mois) | Rendement attendu |
|---|--------|-------------|--------------|-------------------|
| 1 | EcoTech Solutions | 10 000 € | 24 | 12% |
| 2 | Green Energy Maroc | 25 000 € | 36 | 15% |
| 3 | FoodHub Delivery | 5 000 € | 12 | 8% |

---

## 🎯 Enchaînement de la démonstration (15-20 min)

### Phase 1 — Connexion & Navigation (2 min)

1. **Ouvrir l'application** → Écran de login
2. **Se connecter en tant qu'investisseur** : `investor@test.com` / `test123`
3. **Montrer la navigation front-office** : onglets Opportunités, Offres, Portfolio, Risque, Dashboard Avancé
4. **Montrer le chatbot IA** (bouton flottant en bas à droite) → Poser une question : *"Quel secteur est le plus rentable ?"*

> 💡 **Points à souligner** : routage par rôle automatique, UI responsive, chatbot IA accessible partout

---

### Phase 2 — Exploration des Opportunités (2 min)

1. **Aller dans l'onglet Opportunités**
2. **Parcourir les cartes** → Montrer les indicateurs de risque colorés (🟢 Faible / 🟡 Moyen / 🔴 Élevé)
3. **Cliquer sur "Investir"** sur l'opportunité EcoTech Solutions
4. **Remplir le formulaire** : Montant = 15 000 €, Proposition = "Investissement dans l'innovation tech"
5. **Soumettre** → L'offre apparaît en statut "En attente"

> 💡 **Points à souligner** : validation des montants, scoring de risque affiché, UX intuitive

---

### Phase 3 — Analyse de Risque IA (3 min)

1. **Aller dans l'onglet Analyse de Risque**
2. **Remplir le formulaire** :
   - Montant : 15 000 €
   - Durée : 24 mois
   - Secteur : Technologie
3. **Cliquer sur Calculer** → **Gauge animée** qui se remplit progressivement
4. **Observer le score** : ~25/100 (Faible → 🟢)
5. **Cliquer sur "Analyse approfondie IA"** → L'IA LLM génère un paragraphe d'analyse détaillée avec recommandations
6. **Refaire avec montant élevé** : 200 000 € / 36 mois → Score ~55 (Moyen → 🟡)

> 💡 **Points à souligner** : l'API HuggingFace Llama 3.2 est appelée en temps réel, gauge animée par Canvas, la formule de risque combine montant + durée + facteur API externe (Open-Meteo)

---

### Phase 4 — Paiement Stripe (2 min)

1. **Aller dans l'onglet Offres**
2. **L'offre d'Ahmed doit être en statut "Acceptée"** (préparer via back-office admin)
3. **Cliquer sur "Payer"** → Popup avec formulaire de carte
4. **Entrer les données test Stripe** :
   - Carte : `4242 4242 4242 4242`
   - Expiration : `12/27`
   - CVC : `123`
5. **Confirmer le paiement** → Statut passe à "Payé" ✅
6. **L'offre apparaît maintenant dans le Portfolio**

> 💡 **Points à souligner** : API Stripe en mode test, PaymentIntent créé côté serveur, statut mis à jour en base

---

### Phase 5 — Dashboard Avancé (5 min)

#### 5.1 — Smart Contracts
1. **Aller dans Dashboard Avancé → onglet Contrats**
2. **Montrer le contrat auto-généré** pour l'investissement payé
3. **Voir le hash SHA-256** affiché sur le contrat
4. **Signer le contrat** (côté investisseur)
5. **Montrer la vérification d'intégrité** → ✅ Hash vérifié

> 💡 **SHA-256** : chaque contrat contient un hash du contenu. La signature modifie le statut DRAFT → INVESTOR_SIGNED → FULLY_SIGNED

#### 5.2 — AI Matching
1. **Aller dans l'onglet Matching IA**
2. **Créer/modifier le profil investisseur** :
   - Secteurs préférés : Technologie, Énergie
   - Budget : 10 000 – 50 000 €
   - Tolérance risque : Modéré
   - Horizon : 24 mois
3. **Lancer le matching** → L'algorithme affiche les résultats triés par score de compatibilité
4. **Observer les scores** : EcoTech = 87%, Green Energy = 72%, FoodHub = 45%

> 💡 **Algorithme** : scoring pondéré (secteur 35% + budget 25% + risque 25% + horizon 15%), chaque critère normalisé 0-100

#### 5.3 — Portfolio Analytics & Métriques
1. **Aller dans l'onglet Graphiques**
2. **Montrer le KPI Dashboard** en haut :
   - 💰 Capital Total
   - 📊 Nombre de positions + moyenne
   - 🏷 Secteurs actifs + Entropie de Shannon
   - 📐 Indice HHI (concentration)
   - 🎯 Score de diversification (%)
   - ⚡ Risque moyen pondéré
3. **Expliquer les métriques** :
   - *"Le HHI mesure la concentration : < 1500 = diversifié, > 2500 = concentré"*
   - *"L'entropie de Shannon quantifie la qualité de la diversification sectorielle"*
4. **Montrer la Matrice Risque-Rendement** : tableau avec score ajusté par position
5. **Pie Chart** : répartition par secteur
6. **Bar Chart** : montants par projet
7. **Timeline** : évolution cumulative des investissements
8. **Simulateur What-If** :
   - Ajuster le rendement : 12%
   - Ajuster la volatilité : 20%
   - Ajuster l'horizon : 10 ans
   - **Montrer** : CAGR vérifié, Ratio de Sharpe, 3 scénarios (Optimiste/Base/Pessimiste), Drawdown max

> 💡 **Métriques financières réelles** : HHI utilisé par les régulateurs (SEC), Shannon Entropy de la théorie de l'information, Sharpe Ratio standard de la finance quantitative

#### 5.4 — Comparateur
1. **Aller dans l'onglet Comparateur**
2. **Sélectionner 2 opportunités** : EcoTech vs Green Energy
3. **Cliquer sur Comparer** → Tableau comparatif avec :
   - Montants, durées, risques
   - Score global avec le "gagnant" mis en évidence en vert

#### 5.5 — Système de Notation
1. **Aller dans l'onglet Notations**
2. **Noter une opportunité** : EcoTech → 4 étoiles ⭐⭐⭐⭐
3. **Voir la mise à jour** de la moyenne affichée

---

### Phase 6 — Portfolio & Export PDF (2 min)

1. **Aller dans l'onglet Portfolio**
2. **Montrer les cartes** d'investissements confirmés avec statuts
3. **Cliquer sur "Exporter PDF"** → Rapport généré
4. **Montrer la prévisualisation in-app** (PDFBox rendering)

---

### Phase 7 — Back-Office Admin (2 min)

1. **Se déconnecter** et **se reconnecter en ADMIN** (`admin@najahni.com`)
2. **Montrer le Dashboard** : statistiques globales
3. **Montrer le Dashboard Économique** : PIB, inflation, taux de change en temps réel
4. **Montrer la gestion des offres** : accepter/rejeter des offres

---

## ⚠️ Checklist pré-démo

- [ ] MySQL démarré avec `najahni_db` peuplée
- [ ] Au moins 3 projets APPROVED avec opportunités
- [ ] Au moins 1 offre en statut ACCEPTED (pour la démo Stripe)
- [ ] Au moins 2-3 offres PAID (pour avoir des données dans le dashboard)
- [ ] Connexion internet active (APIs HuggingFace, Stripe, World Bank)
- [ ] Tester le paiement Stripe avec carte `4242 4242 4242 4242`
- [ ] Vérifier que le chatbot répond
- [ ] `mvn javafx:run` démarre sans erreur

---

## 🗣 Questions fréquentes et réponses

### Architecture & Choix techniques

**Q: Pourquoi JDBC direct plutôt qu'un ORM comme Hibernate ?**
> Choix académique pour le PIDEV — permet de comprendre les requêtes SQL brutes et la gestion de connexion. Le pattern Service agit comme un DAO simplifié.

**Q: Comment gérez-vous la sécurité des API keys ?**
> En mode développement, les clés sont dans le code source. En production, on utiliserait des variables d'environnement ou un vault (ex: HashiCorp Vault).

**Q: Pourquoi deux services IA (HuggingFace + Gemini) ?**
> Pattern Fallback/Circuit Breaker : HuggingFace Llama 3.2 est le backend principal (gratuit, open-source). Si l'API est down ou rate-limited, on bascule automatiquement sur Google Gemini 2.0 Flash comme fallback.

### Smart Contracts

**Q: Comment fonctionne le hachage SHA-256 des contrats ?**
> À la création du contrat, on concatène toutes les données (montant, parties, dates, termes) et on calcule un hash SHA-256 via `java.security.MessageDigest`. Ce hash est stocké en base. À la vérification, on recalcule le hash et on compare — s'il diffère, le contrat a été altéré.

**Q: C'est quoi la double signature ?**
> Le contrat commence en statut DRAFT. L'investisseur signe (→ INVESTOR_SIGNED), puis l'entrepreneur signe (→ FULLY_SIGNED). Chaque signature met à jour le hash pour garantir l'intégrité.

### Algorithme de Matching

**Q: Comment fonctionne le scoring de matching ?**
> Scoring multi-critères pondéré :
> - **Secteur** (35%) : correspondance exacte des secteurs préférés
> - **Budget** (25%) : proximité entre le budget de l'investisseur et le montant de l'opportunité
> - **Risque** (25%) : correspondance entre la tolérance au risque du profil et le score de risque de l'opportunité
> - **Horizon** (15%) : compatibilité des durées d'investissement
> Chaque critère est normalisé 0-100, le score final est la somme pondérée.

### Métriques financières

**Q: C'est quoi le HHI ?**
> L'indice Herfindahl-Hirschman mesure la concentration. On calcule la part de chaque secteur, on l'élève au carré, et on somme le tout × 10000. < 1500 = diversifié, 1500-2500 = modéré, > 2500 = concentré. C'est utilisé par la SEC américaine.

**Q: Le Ratio de Sharpe, c'est quoi ?**
> C'est la performance ajustée au risque : (rendement - taux sans risque) / volatilité. Un Sharpe > 1 est excellent, > 0.5 est acceptable, < 0 signifie qu'on perd de l'argent par rapport au taux sans risque.

**Q: L'entropie de Shannon dans le contexte financier ?**
> Empruntée à la théorie de l'information, elle mesure le "désordre" de la répartition sectorielle. Une entropie maximale = répartition parfaitement équitable entre les secteurs = diversification optimale. Le score de diversification = entropie réelle / entropie maximale × 100%.

### Paiement Stripe

**Q: Comment fonctionne l'intégration Stripe ?**
> On utilise l'API REST Stripe (pas le SDK) via `HttpClient` natif Java. On crée un PaymentIntent avec le montant, la devise (EUR), et la méthode de paiement. Stripe retourne un `client_secret` qu'on confirme. En mode test, la carte `4242 4242 4242 4242` simule un paiement réussi.

### Analyse de Risque

**Q: Comment le score de risque est-il calculé ?**
> Formule composite : `score = 0.4 × normalisation(montant) + 0.3 × normalisation(durée) + 0.3 × facteur_API`. Le montant est normalisé de 0 à 100 (max = 500k€), la durée de 0 à 100 (max = 60 mois), et le facteur externe est récupéré via l'API Open-Meteo. Le résultat donne un score 0-100 : Faible (0-35), Moyen (36-65), Élevé (66-100).

---

## 📊 Résumé pour la grille de validation

| Critère | Preuve |
|---------|--------|
| **Fonctionnalités avancées (4 pts)** | 10 fonctionnalités avancées fonctionnelles (contrats, matching IA, analytics, comparateur, notation, simulateur, Stripe, risque IA, PDF, chatbot) |
| **Services/API (4 pts)** | 6 APIs externes : Stripe, HuggingFace, Gemini, Exchange Rates, World Bank, Open-Meteo |
| **Scénario & données de test (3 pts)** | Scénario complet avec enchaînement logique et données cohérentes |
| **Maîtrise du sujet (7 pts)** | Documentation des réponses aux questions techniques ci-dessus |
| **Amélioration interfaces (2 pts)** | Refonte visuelle complète v3.0 : palette premium, 2600 lignes CSS, tous inline styles mis à jour |
