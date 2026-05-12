# Najahni -- Application Desktop (JavaFX)

> **Najahni** est une application de bureau riche developpee avec JavaFX 17, miroir fonctionnel de la plateforme web Symfony, offrant une experience native pour la gestion des utilisateurs, l'apprentissage, la communaute, l'investissement, le mentorat et les projets.

---

## Table des matieres

- [Apercu du projet](#apercu-du-projet)
- [Fonctionnalites principales](#fonctionnalites-principales)
- [Technologies utilisees](#technologies-utilisees)
- [Architecture du projet](#architecture-du-projet)
- [Installation](#installation)
- [Configuration](#configuration)
- [Modules](#modules)
- [Integrations IA](#integrations-ia)
- [Securite](#securite)
- [Tests](#tests)
- [Equipe](#equipe)

---

## Apercu du projet

**Najahni** (*"Aide-moi a reussir"*) est l'application desktop JavaFX du projet integre PIDEV 3A a **ESPRIT** (Ecole Superieure Privee d'Ingenierie et de Technologies). Elle partage la meme base de donnees MySQL que la plateforme web Symfony, assurant une synchronisation complete des donnees entre les deux interfaces.

### Objectifs

- Fournir une interface desktop native et performante
- Synchronisation en temps reel avec la base de donnees partagee (najahni_db)
- Integration d'IA hybride (HuggingFace + Gemini) pour l'analyse intelligente
- Tableaux de bord riches avec graphiques et statistiques
- Design unifie reprenant l'identite visuelle de la plateforme web

---

## Fonctionnalites principales

### Gestion des utilisateurs
- Inscription et connexion securisees
- Authentification par reconnaissance faciale (JavaCV / OpenCV)
- Authentification Google OAuth2
- Verification par code email et code aleatoire
- Gestion de profil avec photo de profil
- Changement de mot de passe securise
- Reseau social (followers / following)
- Historique des connexions avec detection de connexions suspectes
- Notifications SMS via **Twilio**
- Panel d'administration back-office complet
- Statistiques utilisateurs avec graphiques

### Apprentissage
- Gestion de cours (CRUD) avec types et niveaux multiples
- Systeme de badges et gamification (XP, progression)
- Commentaires sur les cours
- Visionneuse de documents integree
- Suivi de progression personnalise
- Interface front-office dediee aux apprenants
- Export PDF des cours et rapports

### Communaute
- Gestion de groupes avec demandes d'adhesion
- Fils de discussion (threads) avec creation et edition
- Publications avec reactions
- Evenements communautaires avec details et participation
- Generation de billets QR Code pour les evenements
- Resumes automatiques par IA (AiSummaryService)
- Reponses intelligentes par IA (AiReplyService)
- Suggestions d'evenements par IA (AiEventService)
- Notifications email pour les activites communautaires
- Integration meteo pour les evenements
- Panel d'administration communautaire

### Investissement
- Publication et consultation d'opportunites d'investissement
- Offres d'investissement avec gestion de contrats
- Portefeuille d'investissement personnel
- Analyse de risque economique par IA avec donnees en temps reel
- Matching investisseur-projet intelligent par IA
- Tableau de bord economique (donnees World Bank : PIB, inflation)
- Support multi-devises avec conversion en temps reel
- Gestion des deadlines avec indicateurs visuels
- Paiement securise avec notifications email
- Widget chatbot IA pour conseils d'investissement
- Export PDF des rapports d'investissement
- Geolocalisation des opportunites

### Mentorat
- Gestion des disponibilites des mentors
- Demandes de mentorat avec formulaires
- Sessions de mentorat avec suivi
- Chatbot IA assistant de mentorat
- Notifications email pour les sessions
- Interface d'administration du mentorat

### Gestion de projets
- Creation et suivi de projets avec statuts
- Donnees business associees aux projets
- Scoring IA de la qualite des projets
- Actualites sectorielles via NewsAPI
- Rapports PDF automatiques
- Dashboard client dedie
- Details de projet enrichis

---

## Technologies utilisees

| Categorie | Technologies |
|-----------|-------------|
| **Langage** | Java 17 |
| **UI Framework** | JavaFX 17.0.14, FXML, CSS |
| **Build** | Maven |
| **Base de donnees** | MySQL (Connector/J 8.2.0) |
| **IA / ML** | HuggingFace API (Llama 3.2-3B), Google Gemini 2.0 Flash |
| **Vision par ordinateur** | JavaCV 1.5.10 (OpenCV) |
| **PDF** | iText 7, iText 5, OpenPDF 1.3, Apache PDFBox 2.0 |
| **Excel** | Apache POI 5.2.5 |
| **CSV** | OpenCSV 5.9 |
| **QR Code** | Google ZXing 3.5.3 |
| **SMS** | Twilio SDK 10.1.3 |
| **Email** | Jakarta Mail 2.0.1 |
| **Securite** | BCrypt 0.10.2 |
| **JSON** | Gson 2.10.1, Jackson 2.17.2 |
| **Validation** | ValidatorFX 0.4.0, libphonenumber 8.13 |
| **Styling** | BootstrapFX 0.4.0, CSS3 personnalise |
| **Tests** | JUnit 5.10.2, Mockito 5.11.0 |

---

## Architecture du projet

```
PIDEV-3A-JAVA-Gestion-Users/
|-- src/
|   +-- main/
|       |-- java/
|       |   |-- controllers/           # Controleurs JavaFX
|       |   |   |-- apprentissage/     # Controleurs apprentissage (10+)
|       |   |   |-- community/         # Controleurs communaute (15+)
|       |   |   |-- investissement/    # Controleurs investissement (12+)
|       |   |   |-- mentorat/          # Controleurs mentorat (9+)
|       |   |   +-- projets/           # Controleurs projets (4+)
|       |   |-- models/                # Modeles de donnees (28+)
|       |   |   |-- apprentissage/
|       |   |   |-- community/
|       |   |   |-- investissement/
|       |   |   |-- mentorat/
|       |   |   +-- projets/
|       |   |-- services/              # Services metier (35+)
|       |   |   |-- apprentissage/
|       |   |   |-- community/
|       |   |   |-- investissement/
|       |   |   |-- mentorat/
|       |   |   +-- projets/
|       |   +-- utils/                 # Utilitaires
|       +-- resources/
|           |-- views/                 # Fichiers FXML (50+)
|           |   |-- apprentissage/
|           |   |-- community/
|           |   |-- investissement/
|           |   |-- mentorat/
|           |   +-- projets/
|           |-- css/                   # Feuilles de style
|           +-- images/               # Ressources graphiques
|-- chatbot_api/                      # API Python chatbot (Flask)
|-- rapports_pdf/                     # Rapports PDF generes
|-- pom.xml                           # Configuration Maven
+-- target/                           # Build output
```

---

## Installation

### Prerequis

- **Java JDK** 17+
- **Maven** 3.8+
- **MySQL** / **MariaDB** >= 10.4
- **JavaFX SDK** 17+ (inclus via Maven)

### Etapes d'installation

```bash
# 1. Cloner le depot
git clone https://github.com/votre-repo/PIDEV-3A-JAVA-Gestion-Users.git
cd PIDEV-3A-JAVA-Gestion-Users

# 2. Configurer la base de donnees
# Importer le fichier SQL
mysql -u root -p najahni_db < najahni_db.sql

# 3. Configurer les proprietes
# Editer src/main/resources/db.properties (connexion BDD)
# Editer src/main/resources/secrets.properties (cles API)

# 4. Compiler et lancer
mvn clean javafx:run
```

---

## Configuration

### Base de donnees (`db.properties`)

```properties
db.url=jdbc:mysql://localhost:3306/najahni_db
db.user=root
db.password=
```

### Cles API (`secrets.properties`)

```properties
# HuggingFace (IA principale)
HF_API_KEY=hf_xxx

# Google Gemini (IA fallback)
GEMINI_API_KEY=xxx

# Twilio (SMS)
TWILIO_ACCOUNT_SID=xxx
TWILIO_AUTH_TOKEN=xxx
TWILIO_PHONE_NUMBER=+1xxx

# Google OAuth2
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxx

# Email SMTP
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=xxx@gmail.com
MAIL_PASSWORD=xxx
```

---

## Integrations IA

### Architecture IA hybride

L'application utilise un systeme d'IA a double couche avec basculement automatique :

| Couche | Modele | Utilisation |
|--------|--------|------------|
| **Primaire** | HuggingFace Llama 3.2-3B-Instruct | Analyse de risque, matching, generation de contenu |
| **Fallback** | Google Gemini 2.0 Flash | Backup en cas d'indisponibilite HuggingFace |

### Cas d'utilisation IA

- **Investissement** : Analyse de risque avec facteurs economiques, matching investisseur-projet, insights de projets
- **Communaute** : Resumes automatiques de threads, suggestions de reponses, recommandations d'evenements
- **Projets** : Scoring de qualite, recommandations personnalisees
- **Apprentissage** : Generation de quiz adaptatifs

---

## Modules detailles

### Module Utilisateurs (50+ vues FXML)

| Vue | Description |
|-----|------------|
| SignIn / SignUp | Connexion et inscription avec design moderne |
| FaceLogin | Authentification par reconnaissance faciale |
| Home | Dashboard principal avec navigation par modules |
| Profil | Gestion du profil utilisateur |
| Network | Reseau social (followers, following) |
| AdminBackOffice | Panel d'administration complet |
| Stats | Statistiques et graphiques |

### Module Investissement (12 controleurs)

- Dashboard economique avec donnees en temps reel (World Bank API)
- Portefeuille personnel avec suivi de performance
- Analyse de risque IA avec scoring automatique
- Matching intelligent investisseur - projet
- Chatbot conseiller IA integre
- Contrats numeriques avec gestion des milestones

### Module Communaute (15 controleurs)

- Forum de discussion avec threads et reponses
- Groupes thematiques avec gestion des membres
- Evenements avec billetterie QR Code
- Moderation IA du contenu
- Integration meteo pour les evenements exterieurs

### Module Mentorat (9 controleurs)

- Calendrier de disponibilites
- Workflow complet : demande > validation > session
- Chatbot assistant de mentorat
- Notifications automatiques

### Module Projets (4 controleurs)

- Dashboard de suivi avec indicateurs visuels
- Scoring IA de la maturite des projets
- Rapports PDF automatiques
- Flux d'actualites sectorielles

---

## Securite

- Hachage de mots de passe avec **BCrypt**
- Reconnaissance faciale via **JavaCV / OpenCV**
- Gestion de sessions securisee
- Detection de connexions suspectes
- Validation des numeros de telephone (libphonenumber)
- Cles API externalisees dans `secrets.properties`
- Validation des entrees utilisateur (ValidatorFX)
- Verification par code email (double authentification)

---

## Tests

```bash
# Lancer tous les tests unitaires
mvn test

# Tests avec couverture
mvn test jacoco:report
```

Le projet utilise :
- **JUnit 5** pour les tests unitaires et parametres
- **Mockito** pour le mocking des dependances

---

## Synchronisation Web - Desktop

L'application desktop partage la meme base de donnees MySQL (`najahni_db`) que la plateforme web Symfony. Toute action effectuee sur l'une des interfaces est immediatement visible sur l'autre :

```
+-------------------+     +----------------+     +-------------------+
|  Symfony Web      |---->|  najahni_db    |<----|  JavaFX Desktop   |
|  (PHP 8.2)        |     |  (MySQL)       |     |  (Java 17)        |
+-------------------+     +----------------+     +-------------------+
```

---

## Equipe

Projet developpe par des etudiants de **3eme annee** a **ESPRIT** (Ecole Superieure Privee d'Ingenierie et de Technologies) dans le cadre du module **PIDEV** (Projet Integre de Developpement).

---

## Licence

Ce projet est developpe dans un cadre academique a **ESPRIT**.

---

## Topics et Mots-cles

`javafx` `java` `desktop-application` `education-platform` `e-learning` `mentorship` `investment` `community` `gamification` `ai-powered` `huggingface` `gemini-ai` `face-recognition` `opencv` `javacv` `qr-code` `pdf-generation` `stripe` `twilio-sms` `mysql` `maven` `fxml` `esprit-university` `pidev` `tunisia` `entrepreneurship` `project-management` `risk-analysis` `chatbot`
