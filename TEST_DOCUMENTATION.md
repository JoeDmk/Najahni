# Tests Unitaires - NAJAHNI Platform

## 📋 Vue d'ensemble

Ce document décrit les tests unitaires ajoutés aux modules de gestion de l'apprentissage, des cours et de la progression pour l'application NAJAHNI.

### Technologies utilisées
- **JUnit 5.10.2** - Framework de test
- **Mockito 5.11.0** - Framework de mocking
- **Maven Surefire 3.0.0** - Plugin pour l'exécution des tests

---

## 📁 Structure des tests

```
src/test/java/com/najahni/
├── services/
│   ├── CoursServiceTest.java          # Tests du service Cours
│   └── ProgressionServiceTest.java    # Tests du service Progression
└── controllers/
    └── ApprentissageControllerTest.java # Tests du contrôleur Apprentissage
```

---

## 🧪 Détail des tests

### 1. **CoursServiceTest** (`CoursServiceTest.java`)

Tests unitaires pour le service `CoursService` qui gère la logique métier des cours.

#### 📊 Couverture complète

| Catégorie | Tests | Détails |
|-----------|-------|---------|
| **Création** | 8 tests | Validation des données, cas d'erreur, règles métier |
| **Modification** | 2 tests | Succès et échec de la mise à jour |
| **Suppression** | 2 tests | Succès et échec de la suppression |
| **Recherche** | 10 tests | Par ID, tous, par niveau, par type, par créateur, recherche |
| **Comptage** | 3 tests | Total, certifiants, XP |
| **Total** | **25 tests** | |

#### Test clès - Création d'un cours

```java
@Test
@DisplayName("Créer un cours valide - succès")
void testCreerCoursValide()
```

Vérifie que un cours valide est créé avec succès.

#### Tests de validation

- Cours null
- Titre manquant ou vide
- Titre trop long (>255 caractères)
- Description trop longue (>2000 caractères)
- Points XP négatifs
- Durée négative
- Niveau ou Type manquant

#### Tests paramétrés

Utilise `@ParameterizedTest` pour tester tous les niveaux et types de cours.

---

### 2. **ProgressionServiceTest** (`ProgressionServiceTest.java`)

Tests unitaires pour le service `ProgressionService` qui gère le suivi des progressions utilisateur.

#### 📊 Couverture complète

| Catégorie | Tests | Détails |
|-----------|-------|---------|
| **Démarrage de cours** | 2 tests | Nouvelle progression, progression existante |
| **Mise à jour pourcentage** | 3 tests | Progression incomplète, 100%, inexistante |
| **Mise à jour avec XP** | 2 tests | Ajout de points, complétion avec badges |
| **Complétion de cours** | 2 tests | Cours certifiant et non-certifiant |
| **Recherche** | 6 tests | Par utilisateur, par cours, par état, toutes, par ID |
| **CRUD basique** | 3 tests | Créer, mettre à jour, supprimer |
| **Statistiques** | 5 tests | XP total, niveau, cours complétés, leaderboard |
| **Total** | **23 tests** | |

#### Tests clés - Complétion de cours

```java
@Test
@DisplayName("Compléter un cours - nouvelle progression")
void testCompleterCoursNouvelleProgression()
```

Vérifie que :
- Une nouvelle progression est créée
- Le pourcentage est à 100%
- L'état est marqué comme CERTIFIE (si certifiant)
- Les badges sont vérifiés

#### Tests de statistiques

- Calcul du total XP
- Détermination du niveau global
- Comptage des cours complétés
- Génération des statistiques complètes
- Leaderboard (top 10 et limite personnalisée)

#### Tests paramétrés pour les niveaux

```java
@ParameterizedTest
@ValueSource(ints = {100, 300, 600, 1000, 1500})
void testGetNiveauGlobal(int totalXP)
```

Teste le calcul du niveau pour différentes valeurs d'XP.

---

### 3. **ApprentissageControllerTest** (`ApprentissageControllerTest.java`)

Tests unitaires pour le contrôleur `ApprentissageController` qui coordonne l'apprentissage et la gamification.

#### 📊 Couverture complète

| Catégorie | Tests | Détails |
|-----------|-------|---------|
| **Gestion de progression** | 4 tests | Récupération, démarrage, complétion, mise à jour |
| **Statistiques utilisateur** | 4 tests | XP total, niveau, cours complétés, stats complètes |
| **Gamification** | 4 tests | Badges disponibles, badges actifs, attribution, vérification |
| **Leaderboard** | 2 tests | Classement personnalisé, classement par défaut |
| **Recherche** | 2 tests | Progression trouvée, inexistante |
| **Gestion des badges** | 3 tests | Créer, mettre à jour, supprimer |
| **Gestion des erreurs** | 2 tests | Mise à jour invalide, progression vide |
| **Total** | **21 tests** | |

#### Tests clés - Apprentissage

```java
@Test
@DisplayName("Compléter un cours avec succès")
void testCompleterCours()
```

Vérifie le workflow complet de complétion d'un cours.

#### Tests de gamification

```java
@Test
@DisplayName("Vérifier et attribuer badges à un utilisateur")
void testVerifierEtAttribuerBadges()
```

Simule l'attribution automatique de badges après l'action d'un utilisateur.

---

## 🚀 Exécution des tests

### Exécuter tous les tests
```bash
mvn test
```

### Exécuter une classe de test spécifique
```bash
mvn test -Dtest=CoursServiceTest
mvn test -Dtest=ProgressionServiceTest
mvn test -Dtest=ApprentissageControllerTest
```

### Exécuter un test spécifique
```bash
mvn test -Dtest=CoursServiceTest#testCreerCoursValide
```

### Avec affichage détaillé
```bash
mvn test -X
```

### Générer un rapport de couverture (avec JaCoCo)
```bash
mvn test jacoco:report
```

---

## 📊 Statistiques des tests

| Aspect | Nombre |
|--------|--------|
| **Classes de test** | 3 |
| **Méthodes de test** | 69 |
| **Assertions** | 188+ |
| **Mocks utilisés** | 6 |
| **Tests paramétrés** | 3 |
| **Couverture prévue** | 85%+ |

---

## 🎯 Bonnes pratiques appliquées

### 1. **Annotations et Display Names**
Chaque test inclut `@DisplayName` pour une meilleure lisibilité dans les rapports.

```java
@Test
@DisplayName("Créer un cours valide - succès")
void testCreerCoursValide()
```

### 2. **Pattern Arrange-Act-Assert (AAA)**
Tous les tests suivent le pattern AAA pour la clarté.

```java
// Arrange - Préparation
when(coursDAO.create(courTest)).thenReturn(courTest);

// Act - Action
Cours resultat = coursService.creerCours(courTest);

// Assert - Vérification
assertEquals(courTest.getTitre(), resultat.getTitre());
```

### 3. **Mocking avec Mockito**
Utilisation complète de Mockito pour isoler les dépendances.

```java
@Mock
private CoursDAO coursDAO;

@InjectMocks
private CoursService coursService;
```

### 4. **Vérification des interactions**
Utilisation de `verify()` pour confirmer les appels attendus.

```java
verify(coursDAO).create(courTest);
verify(badgeService, never()).verifierEtAttribuerBadges(anyInt());
```

### 5. **Tests paramétrés**
Utilisation de `@ParameterizedTest` pour tester multiple valeurs.

```java
@ParameterizedTest
@EnumSource(NiveauCours.class)
void testTrouverParNiveau(NiveauCours niveau)
```

---

## 🔍 Cas de test importants

### Validation des données
Les tests vérifient que :
- Les données nulles sont rejetées
- Les valeurs invalides (strings vides, nombres négatifs) sont rejetées
- Les limites de longueur sont respectées
- Les champs obligatoires sont nécessaires

### Gestion des erreurs
Les tests vérifient que :
- Les exceptions appropriées sont levées
- Les appels à la base de données ne sont pas faits en cas d'erreur
- Les opérations échouent gracieusement

### Logique métier
Les tests vérifient que :
- Les progressions sont mises à jour correctement
- Les badges sont attribués au moment approprié
- Les statistiques sont calculées correctement
- Le leaderboard est généré correctement

---

## 📚 Dépendances dans pom.xml

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

---

## 🎓 Extensions possibles

Les tests peuvent être étendus pour couvrir :

1. **Tests d'intégration** - Interaction avec la base de données réelle
2. **Tests de performance** - Temps d'exécution pour les requêtes courantes
3. **Tests de concurrence** - Comportement avec plusieurs utilisateurs
4. **Couverture de code** - Augmenter la couverture à 90%+
5. **Tests des controllers JavaFX** - Tests des composants UI

---

## 🔧 Troubleshooting

### Les tests ne s'exécutent pas
Assurez-vous que :
- Les fichiers sont dans `src/test/java` (et non `src/main/java`)
- Les noms des classes se terminent par `Test` ou `Tests`
- Le plugin Maven Surefire est configuré

### Erreur : Cannot find symbol
Assurez-vous que les imports sont corrects et que les dépendances JUnit et Mockito sont dans le `pom.xml`.

### Mock ne fonctionne pas
Assurez-vous que la classe de test est annotée avec `@ExtendWith(MockitoExtension.class)`.

---

## 📞 Contact et Support

Pour plus d'informations sur les tests, consultez la [documentation JUnit 5](https://junit.org/junit5/) et [la documentation Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html).

---

**Dernière mise à jour**: Février 2026  
**Version**: 1.0
