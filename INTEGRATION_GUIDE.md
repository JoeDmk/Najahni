# Najahni - Guide d'Intégration des Modules

## Vue d'ensemble

Ce projet regroupe les 6 modules de l'application Najahni. Chaque membre de l'équipe doit adapter son code pour l'intégrer dans la structure unifiée.

| Module | Responsable | Branche d'origine | Sous-package |
|--------|-------------|-------------------|--------------|
| Gestion des Utilisateurs | Ilyess | `gestion-users` | *(racine)* – déjà en place |
| Gestion des Projets | Mahdi | `gestion-projets` | `projets` |
| Investissement | Youcef | `investissement` | `investissement` |
| Community & Forum | Dali | `Community` | `community` |
| Processus Mentorat | Neirouz | `processus-mentorat` | `mentorat` |
| Apprentissage & Gamification | Isra | `Apprentissage&Gamification` | `apprentissage` |

---

## 1. Base de données

Tout le monde utilise la **même base de données** : `najahni_db`

### Setup
```sql
-- Dans phpMyAdmin ou MySQL CLI :
SOURCE najahni_db.sql;
```

Le fichier `najahni_db.sql` contient TOUTES les tables de tous les modules. Exécutez-le une seule fois.

### Connexion
- **Host** : `localhost:3306`
- **DB** : `najahni_db`
- **User** : `root`
- **Password** : *(vide)*

---

## 2. Structure du projet

```
src/main/java/
├── controllers/                    ← Contrôleurs Users (Ilyess)
│   ├── projets/                   ← Contrôleurs Projets (Mahdi)
│   ├── investissement/            ← Contrôleurs Investissement (Youcef)
│   ├── community/                 ← Contrôleurs Community (Dali)
│   ├── mentorat/                  ← Contrôleurs Mentorat (Neirouz)
│   └── apprentissage/             ← Contrôleurs Apprentissage (Isra)
│
├── models/                         ← Modèles Users
│   ├── projets/                   ← Modèles Projets
│   ├── investissement/            ← Modèles Investissement
│   ├── community/                 ← Modèles Community
│   ├── mentorat/                  ← Modèles Mentorat
│   └── apprentissage/             ← Modèles Apprentissage
│
├── services/                       ← Services Users
│   ├── projets/                   ← Services Projets
│   ├── investissement/            ← Services Investissement
│   ├── community/                 ← Services Community
│   ├── mentorat/                  ← Services Mentorat
│   └── apprentissage/             ← Services Apprentissage
│
├── interfaces/                     ← Interfaces Users
│   ├── projets/
│   ├── investissement/
│   ├── community/
│   ├── mentorat/
│   └── apprentissage/
│
├── tools/
│   └── MyConnection.java          ← ⚠️ CLASSE UNIQUE - NE PAS MODIFIER
│
├── exceptions/                     ← Exceptions partagées
└── test/                           ← Classes de test / Main
```

### Ressources (FXML + CSS)
```
src/main/resources/
├── views/                          ← FXML Users (existants)
│   ├── projets/                   ← FXML Projets
│   ├── investissement/            ← FXML Investissement
│   ├── community/                 ← FXML Community
│   ├── mentorat/                  ← FXML Mentorat
│   └── apprentissage/             ← FXML Apprentissage
├── css/                            ← CSS partagés
└── i18n/                           ← Traductions
```

---

## 3. Instructions par membre

### ⚠️ Règles communes pour TOUT LE MONDE

1. **Supprimer** votre classe de connexion (`MyBD`, `DBConnection`, `DataBase`, etc.)
2. **Utiliser** `tools.MyConnection` partout :
   ```java
   import tools.MyConnection;
   
   // Dans votre service :
   private Connection connection = MyConnection.getInstance().getConnection();
   ```
3. **Changer** le `package` de chaque fichier Java
4. **Ne PAS** ajouter de `module-info.java`
5. **Ne PAS** modifier le `pom.xml` sans accord de l'équipe

---

### Mahdi (Gestion des Projets)

**Copier vos fichiers dans :**

| Fichier source | Destination | Nouveau package |
|---|---|---|
| `AjouterProjet.java` | `controllers/projets/` | `package controllers.projets;` |
| `ClientDashboardController.java` | `controllers/projets/` | `package controllers.projets;` |
| `Projet.java` | `models/projets/` | `package models.projets;` |
| `donneesBusiness.java` | `models/projets/` | `package models.projets;` |
| `projetCRUD.java` | `services/projets/` | `package services.projets;` |
| `donneesBusinessCRUD.java` | `services/projets/` | `package services.projets;` |
| `intrefaceCRUD.java` | `interfaces/projets/` | `package interfaces.projets;` |

**FXML :** Copier dans `src/main/resources/views/projets/`

**Modifications requises :**
```java
// AVANT (votre code actuel)
import edu.najahni.tools.MyBD;
private Connection conn = MyBD.getInstance().getConn();

// APRÈS 
import tools.MyConnection;
private Connection connection = MyConnection.getInstance().getConnection();
```

---

### Youcef (Investissement)

**Copier vos fichiers dans :**

| Fichier source | Destination | Nouveau package |
|---|---|---|
| `InvestmentOffer.java` | `models/investissement/` | `package models.investissement;` |
| `InvestmentOpportunity.java` | `models/investissement/` | `package models.investissement;` |
| `OfferStatus.java` | `models/investissement/` | `package models.investissement;` |
| `OpportunityStatus.java` | `models/investissement/` | `package models.investissement;` |
| `InvestmentOfferService.java` | `services/investissement/` | `package services.investissement;` |
| `InvestmentOpportunityService.java` | `services/investissement/` | `package services.investissement;` |
| `RiskService.java`, `RiskCalculator.java`, etc. | `services/investissement/` | `package services.investissement;` |
| `EconomicApiService.java`, `CurrencyService.java` | `services/investissement/` | `package services.investissement;` |
| `PaymentService.java`, `DeadlineService.java` | `services/investissement/` | `package services.investissement;` |
| `InvestmentOfferController.java` | `controllers/investissement/` | `package controllers.investissement;` |
| `InvestmentOpportunityController.java` | `controllers/investissement/` | `package controllers.investissement;` |
| `EconomicDashboardController.java` | `controllers/investissement/` | `package controllers.investissement;` |
| `FrontOffersController.java`, etc. | `controllers/investissement/` | `package controllers.investissement;` |

**FXML :** Copier dans `src/main/resources/views/investissement/`

**Modifications requises :**
```java
// AVANT
import com.najahni.utils.DBConnection;
private Connection cnx = DBConnection.getInstance().getConnection();

// APRÈS
import tools.MyConnection;
private Connection connection = MyConnection.getInstance().getConnection();
```

**Important :** Supprimer `module-info.java`. Ne copier ni `User.java` ni `Project.java` (ces modèles existent déjà dans le module users/projets).

---

### Dali (Community & Forum)

**Copier vos fichiers dans :**

| Type | Fichiers | Destination | Nouveau package |
|---|---|---|---|
| Models | `Event`, `EventParticipant`, `Group`, `GroupMember`, `GroupJoinRequest`, `Post`, `Thread`, `Comment` | `models/community/` | `package models.community;` |
| Services | `EventCRUD`, `PostCRUD`, `ThreadCRUD`, `CommentCRUD`, `GroupCRUD`, `GroupMemberCRUD`, `GroupJoinRequestCRUD`, `EventParticipantCRUD`, `PostReactionCRUD`, `AiEventService`, `AiReplyService`, `AiSummaryService`, `QrUtil`, `QrDecodeUtil`, `TicketSigner`, `WeatherService` | `services/community/` | `package services.community;` |
| Controllers | `CommunityHomeController`, `EventsPageController`, `PostsPageController`, etc. | `controllers/community/` | `package controllers.community;` |
| Interfaces | `IntrefaceCRUD` | `interfaces/community/` | `package interfaces.community;` |

**FXML :** Copier dans `src/main/resources/views/community/`

**Modifications requises :**
```java
// AVANT
import Utils.MyBD;
private Connection conn = MyBD.getInstance().getConn();

// APRÈS
import tools.MyConnection;
private Connection connection = MyConnection.getInstance().getConnection();
```

---

### Neirouz (Processus Mentorat)

**Copier vos fichiers dans :**

| Type | Fichiers | Destination | Nouveau package |
|---|---|---|---|
| Models | `MentorAvailability`, `MentorshipRequest`, `MentorshipSession` | `models/mentorat/` | `package models.mentorat;` |
| Services | `ServiceMentorAvailability`, `ServiceMentorshipRequest`, `ServiceMentorshipSession` | `services/mentorat/` | `package services.mentorat;` |
| Interfaces | `IMentorAvailability`, `IMentorshipRequest`, `IMentorshipSession`, `IService` | `interfaces/mentorat/` | `package interfaces.mentorat;` |
| Controllers | `MentorAvailabilityFormController`, `MentorAvailabilityListController`, `MentorshipRequestFormController`, `MentorshipRequestListController`, `MentorshipSessionFormController`, `MentorshipSessionListController`, `ChatbotController` | `controllers/mentorat/` | `package controllers.mentorat;` |

**FXML :** Copier depuis `src/main/resources/FXML/` vers `src/main/resources/views/mentorat/`

**Modifications requises :**
```java
// AVANT
import tn.esprit.utils.DataBase;
private Connection cnx = DataBase.getInstance().getCnx();

// APRÈS
import tools.MyConnection;
private Connection connection = MyConnection.getInstance().getConnection();
// OU si vous préférez garder le nom cnx :
private Connection cnx = MyConnection.getInstance().getCnx();
```

**Note :** Ne copier ni `User.java` ni `Projet.java` depuis votre branche (ils existent déjà).

**Chatbot API :** Le dossier `chatbot_api/` (Python Flask) doit être copié à la racine du projet.

---

### Isra (Apprentissage & Gamification)

**Copier vos fichiers dans :**

| Type | Fichiers | Destination | Nouveau package |
|---|---|---|---|
| Models | `Cours`, `Progression`, `Badge`, `EtatProgression`, `NiveauCours`, `TypeCours` | `models/apprentissage/` | `package models.apprentissage;` |
| Services | `CoursService`, `ProgressionService`, `BadgeService` | `services/apprentissage/` | `package services.apprentissage;` |
| Controllers | `ApprentissageController`, `CoursController` | `controllers/apprentissage/` | `package controllers.apprentissage;` |

**FXML :** Copier dans `src/main/resources/views/apprentissage/`

**Modifications requises :**
```java
// AVANT
import com.najahni.utils.DBConnection;
private Connection cnx = DBConnection.getInstance().getConnection();

// APRÈS
import tools.MyConnection;
private Connection connection = MyConnection.getInstance().getConnection();
```

**Important :** Supprimer `module-info.java`. Ne copier ni `User.java`, `Project.java`, `InvestmentOpportunity.java` (ils existent dans d'autres modules).

---

## 4. Ajuster les chemins FXML dans les contrôleurs

Quand vous naviguez vers une vue d'un autre module, utilisez le chemin complet :

```java
// Charger un FXML de votre module (ex: community)
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/EventsPage.fxml"));

// Charger un FXML du module users (existant)
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));

// Charger un FXML du module projets
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/projets/AjoutProjet.fxml"));
```

---

## 5. Modèle User partagé

Le modèle `User` principal est dans `models/User.java` (module d'Ilyess). Si votre code utilise son propre `User.java` simplifié, **supprimez-le** et importez depuis `models.User`.

```java
import models.User;
```

---

## 6. Checklist avant de soumettre votre code

- [ ] Tous les fichiers sont dans les bons sous-packages
- [ ] Le `package` en haut de chaque fichier est correct
- [ ] Plus aucune référence à `MyBD`, `DBConnection`, ou `DataBase` — tout utilise `tools.MyConnection`
- [ ] Pas de `module-info.java`
- [ ] Les imports sont mis à jour (pas d'ancien package comme `edu.najahni`, `com.najahni`, `tn.esprit`, etc.)
- [ ] Les chemins FXML dans les contrôleurs utilisent `/views/<module>/`
- [ ] Votre code compile sans erreurs (`mvn compile`)
- [ ] La base de données `najahni_db` a été recréée avec le nouveau `najahni_db.sql`

---

## 7. Compiler le projet

```bash
mvn clean compile
```

Si tout est bien intégré, le projet devrait compiler sans erreurs.
