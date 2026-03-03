# NAJAHNI - Module Mentorat : Scenario de Test et Documentation Fonctionnelle
## Sprint JavaFX - Validation Individuelle

---

## Table des matieres

1. [Presentation du module](#1-presentation-du-module)
2. [Architecture technique](#2-architecture-technique)
3. [Modeles de donnees](#3-modeles-de-donnees)
4. [Services et APIs externes integres](#4-services-et-apis-externes-integres)
5. [Fonctionnalites avancees implementees](#5-fonctionnalites-avancees-implementees)
6. [Interfaces graphiques](#6-interfaces-graphiques)
7. [Scenarios de test utilisateur](#7-scenarios-de-test-utilisateur)
8. [Donnees de test](#8-donnees-de-test)
9. [Concepts theoriques appliques](#9-concepts-theoriques-appliques)

---

## 1. Presentation du module

Le module **Mentorat** de la plateforme NAJAHNI permet la mise en relation entre
**entrepreneurs** et **mentors** dans un ecosysteme fintech. Il couvre l'ensemble du
cycle de vie du mentorat :

- Gestion des disponibilites mentor
- Creation et suivi des demandes de mentorat
- Planification et gestion des sessions
- Systeme de feedback et d'evaluation
- Notifications automatiques (Email + SMS)
- Chatbot IA d'assistance
- Export de rapports (PDF + Excel avec analyse IA)
- Tableau de bord administrateur avec statistiques et graphiques

### Roles utilisateur

| Role | Description | Acces |
|------|-------------|-------|
| **ENTREPRENEUR** | Demandeur de mentorat | Front-office : Explorer mentors, Mes demandes, Mes sessions |
| **MENTOR** | Offre son expertise | Front-office : Explorer mentors, Demandes recues, Mes sessions, Mes disponibilites |
| **ADMIN** | Gestion globale | Back-office : Dashboard, CRUD Demandes, Sessions, Disponibilites |

---

## 2. Architecture technique

### Stack technologique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 17 |
| Framework UI | JavaFX | 17 |
| Build | Maven | 3.x |
| Base de donnees | MySQL | 8.x |
| Connexion JDBC | `tools.MyConnection` (Singleton) | UTF-8 |

### Structure du module

```
src/main/java/
  controllers/mentorat/
    FrontMentoratController.java          (1042 lignes - Front-office complet)
    MentoratAdminController.java          (1270+ lignes - Back-office admin)
    ChatbotController.java                (Chatbot IA)
    MentorshipRequestListController.java  (595 lignes - Liste demandes)
    MentorshipRequestFormController.java  (Formulaire + Speech-to-Text)
    MentorshipSessionListController.java  (680 lignes - Sessions + Export PDF/Excel/IA)
    MentorshipSessionFormController.java  (Formulaire sessions)
    MentorAvailabilityListController.java (Liste disponibilites)
    MentorAvailabilityFormController.java (Formulaire disponibilites)

  models/mentorat/
    MentorAvailability.java               (Creneaux de disponibilite)
    MentorshipRequest.java                (Demandes de mentorat)
    MentorshipSession.java                (Sessions de mentorat)
    Projet.java                           (Reference projet)

  services/mentorat/
    ServiceMentorAvailability.java        (CRUD disponibilites)
    ServiceMentorshipRequest.java         (CRUD demandes + auto-session)
    ServiceMentorshipSession.java         (CRUD sessions)
    ServiceProjet.java                    (Lecture projets)
    ServiceUser.java                      (Utilisateurs + filtrage mentors)
    MentoratNotificationService.java      (Notifications Email + SMS)

  util/mentorat/
    AudioTranscriber.java                 (Enregistrement micro + Whisper STT)
    StageManager.java                     (Gestion fenetres)

  resources/views/mentorat/
    FrontMentorat.fxml                    (Vue front-office)
    MentoratAdmin.fxml                    (Vue back-office)
    Chatbot.fxml                          (Vue chatbot)
    MentorshipRequestList/Form.fxml       (Vues demandes)
    MentorshipSessionList/Form.fxml       (Vues sessions)
    MentorAvailabilityList/Form.fxml      (Vues disponibilites)
    mentorat.css                          (Styles CSS dedies)
```

---

## 3. Modeles de donnees

### 3.1 MentorAvailability

| Champ | Type | Description |
|-------|------|-------------|
| id | int | Identifiant unique |
| mentorId | int | FK -> user.id |
| date | Date | Date du creneau |
| startTime | Time | Heure de debut |
| endTime | Time | Heure de fin |
| createdAt | Timestamp | Date de creation |
| mentorName | String | Nom affichage (JOIN) |

### 3.2 MentorshipRequest

| Champ | Type | Description |
|-------|------|-------------|
| id | int | Identifiant unique |
| entrepreneurId | int | FK -> user.id |
| mentorId | int | FK -> user.id |
| projectId | int | FK -> projet.id |
| date | Date | Date souhaitee |
| time | String | Heure souhaitee |
| motivation | String | Motivation de l'entrepreneur |
| goals | String | Objectifs du mentorat |
| matchScore | float | Score de compatibilite (0-1) |
| autoApproved | boolean | Approbation automatique |
| status | RequestStatus | auto_accepted, pending_review, rejected, cancelled, completed |
| createdAt | Timestamp | Date de creation |
| updatedAt | Timestamp | Derniere modification |
| entrepreneurName | String | Nom affichage (JOIN) |
| mentorName | String | Nom affichage (JOIN) |
| projectName | String | Titre projet (JOIN) |

### 3.3 MentorshipSession

| Champ | Type | Description |
|-------|------|-------------|
| id | int | Identifiant unique |
| requestId | int | FK -> mentorship_request.id |
| scheduledAt | Timestamp | Date/heure de la session |
| durationMinutes | int | Duree en minutes |
| status | SessionStatus | scheduled, completed, cancelled, no_show |
| mentorFeedback | String | Feedback du mentor |
| entrepreneurFeedback | String | Feedback de l'entrepreneur |
| mentorRating | int | Note mentor (1-5) |
| entrepreneurRating | int | Note entrepreneur (1-5) |
| meetingLink | String | URL de reunion |
| createdAt | Timestamp | Date de creation |
| updatedAt | Timestamp | Derniere modification |

---

## 4. Services et APIs externes integres

### 4.1 Groq AI - LLaMA 3.3 70B (API REST HTTPS)

| Detail | Valeur |
|--------|--------|
| **Endpoint** | `https://api.groq.com/openai/v1/chat/completions` |
| **Modele** | `llama-3.3-70b-versatile` |
| **Authentification** | Bearer token (cle API dans `secrets.properties`) |
| **Utilisation** | Generation automatique d'insights IA dans l'export Excel |
| **Prompt** | Analyse des statistiques de sessions : nombre total, repartition statuts, duree totale, notes moyennes, feedbacks. Demande : resume, observations cles, recommandations, alertes |
| **Integration** | `MentorshipSessionListController.callAIForInsights()` |

### 4.2 FastAPI Chatbot (API REST HTTP locale)

| Detail | Valeur |
|--------|--------|
| **Endpoint** | `POST http://localhost:8001/chat` |
| **Format requete** | `{"question": "..."}` (JSON) |
| **Format reponse** | `{"answer": "..."}` (JSON) |
| **Utilisation** | Chatbot IA interactif pour questions sur le mentorat |
| **Integration** | `ChatbotController.java` - appel asynchrone (thread) |

### 4.3 FastAPI Whisper STT (Speech-to-Text, API REST HTTP locale)

| Detail | Valeur |
|--------|--------|
| **Endpoint** | `POST http://localhost:8001/transcribe` |
| **Format requete** | Multipart form-data (`file`: WAV 16kHz mono 16-bit) |
| **Format reponse** | `{"text": "..."}` (JSON) |
| **Utilisation** | Transcription vocale dans les champs motivation et objectifs |
| **Integration** | `AudioTranscriber.java` - toggle enregistrement micro + upload |
| **Format audio** | PCM WAV : 16000 Hz, 16-bit, mono, signé, big-endian |

### 4.4 iText 5 - Generation PDF

| Detail | Valeur |
|--------|--------|
| **Librairie** | `com.itextpdf:itextpdf` (Maven) |
| **Utilisation** | Export PDF des sessions avec mise en forme professionnelle |
| **Fonctionnalites** | Tableau 10 colonnes, couleurs alternees, en-tete gradient, apercu in-app |
| **Integration** | `MentorshipSessionListController.generateStyledPDF()` |

### 4.5 Apache POI - Export Excel XLSX

| Detail | Valeur |
|--------|--------|
| **Librairie** | `org.apache.poi:poi-ooxml` (Maven) |
| **Utilisation** | Export Excel multi-feuilles avec statistiques et analyse IA |
| **Feuille 1** | "Sessions" - Tableau 11 colonnes avec styles (en-tetes bleus, bordures) |
| **Feuille 2** | "AI Insights" - Statistiques rapides + analyse IA generee par Groq LLaMA |
| **Integration** | `MentorshipSessionListController.handleExportExcel()` |

### 4.6 Twilio SMS API (API REST)

| Detail | Valeur |
|--------|--------|
| **Service** | Twilio Programmable SMS |
| **Authentification** | Account SID + Auth Token (dans `secrets.properties`) |
| **Utilisation** | Notification SMS : nouvelle demande (au mentor), changement statut (a l'entrepreneur) |
| **Code pays** | +216 (Tunisie) par defaut |
| **Integration** | `MentoratNotificationService.sendSms()` - appel asynchrone |

### 4.7 Jakarta Mail - Email SMTP (Gmail)

| Detail | Valeur |
|--------|--------|
| **Protocole** | SMTP avec STARTTLS |
| **Serveur** | `smtp.gmail.com:587` |
| **Authentification** | App Password Gmail (dans `secrets.properties`) |
| **Templates** | HTML stylise avec header gradient orange, body blanc, footer gris |
| **3 types d'emails** | Changement de statut, Nouvelle demande, Session terminee |
| **Integration** | `MentoratNotificationService.sendEmail()` - appel asynchrone |

### 4.8 BootstrapFX (Librairie CSS JavaFX)

| Detail | Valeur |
|--------|--------|
| **Librairie** | `org.kordamp.bootstrapfx:bootstrapfx-core` |
| **Utilisation** | Classes CSS Bootstrap adaptees pour JavaFX |

### Resume des APIs (8 total)

| # | API/Service | Type | Protocole |
|---|-------------|------|-----------|
| 1 | Groq AI (LLaMA 3.3 70B) | Cloud AI | HTTPS REST |
| 2 | FastAPI Chatbot | Backend local | HTTP REST |
| 3 | FastAPI Whisper STT | Backend local | HTTP Multipart |
| 4 | iText PDF | Librairie Java | Local |
| 5 | Apache POI Excel | Librairie Java | Local |
| 6 | Twilio SMS | Cloud SaaS | HTTPS REST |
| 7 | Jakarta Mail SMTP | Email | SMTP/TLS |
| 8 | BootstrapFX | Librairie CSS | Local |

---

## 5. Fonctionnalites avancees implementees

### 5.1 Chatbot IA interactif
- Interface de chat temps reel avec un assistant IA
- Appel API asynchrone (thread separee pour ne pas bloquer l'UI)
- Historique des messages affiche dans la conversation
- Utilise FastAPI + modele NLP en backend

### 5.2 Speech-to-Text (Dictee vocale)
- Bouton microphone sur les champs Motivation et Objectifs du formulaire de demande
- Toggle : appui 1 = debut enregistrement (bouton rouge), appui 2 = arret + transcription
- Enregistrement WAV via `javax.sound.sampled` (16kHz, 16-bit, mono)
- Upload multipart vers API Whisper locale
- Le texte transcrit s'ajoute au contenu existant du champ
- Indication visuelle : "Stop" (rouge), "Transcription..." (ambre), "Record" (normal)

### 5.3 Export Excel avec analyse IA (Groq LLaMA 3.3 70B)
- Export Excel multi-feuilles : donnees + insights IA
- Feuille 1 : sessions formatees avec en-tetes bleus et bordures
- Feuille 2 : statistiques rapides + analyse complete generee par IA
- Le prompt envoie : totaux, repartition statuts, duree, notes moyennes, feedbacks
- L'IA retourne : resume, observations, recommandations, alertes
- Export asynchrone avec indicateur "Exporting..." sur le bouton

### 5.4 Export PDF avec apercu in-app
- Generation PDF A4 paysage avec iText
- Tableau colore (alternance blanc/gris clair, en-tetes bleus)
- Branding Najahni : titre, horodatage, compteur de sessions
- Apercu integre dans l'application (popup) sans ouvrir un lecteur externe

### 5.5 Notifications Email + SMS automatiques
- **Nouvelle demande** : email + SMS au mentor l'informant de la demande
- **Changement de statut** : email + SMS a l'entrepreneur quand sa demande est acceptee/rejetee
- **Session completee** : email de rappel au mentor et a l'entrepreneur pour laisser un feedback
- Emails HTML styles avec branding Najahni (gradient orange, mise en forme professionnelle)
- Tous les envois sont asynchrones (threads nommees) pour ne pas bloquer l'UI
- Twilio pour SMS (code pays +216 Tunisie), Gmail SMTP pour email

### 5.6 Tableau de bord avec graphiques JavaFX
- **5 stat cards animes** : Demandes, En attente, Sessions, Completees, Disponibilites
- Animation d'entree echelonnee (fade + slide, 60ms entre chaque carte)
- Effet hover avec changement de couleur
- **PieChart** : Repartition des demandes par statut (5 couleurs, legende en bas)
- **BarChart** : Sessions par mois (6 derniers mois, noms de mois en francais)
- Activites recentes : 5 dernieres demandes avec badges de statut colores

### 5.7 Systeme de feedback et notation
- Notes de 1 a 5 pour mentor et entrepreneur apres chaque session
- Commentaires textuels bidirectionnels (mentor feedback + entrepreneur feedback)
- Integre dans le formulaire de session et la vue listing

### 5.8 Auto-creation de session
- Quand une demande est approuvee automatiquement (autoApproved=true)
- Le service cree automatiquement une session planifiee avec la date/heure de la demande
- Duree par defaut : 60 minutes
- Declenchement dans `ServiceMentorshipRequest.add()`

### 5.9 Filtrage dynamique des mentors
- Filtre par date + heure en front-office (Tab "Explorer les mentors")
- Requete SQL avec JOIN sur `mentor_availability` pour ne montrer que les mentors disponibles
- Deux variantes : filtrage precis (date+heure) ou affichage de tous les mentors disponibles

### 5.10 Detection de chevauchement
- Lors de l'ajout/modification d'un creneau de disponibilite
- Verification automatique des chevauchements avec les creneaux existants du meme mentor
- Message d'erreur explicite si un chevauchement est detecte

### 5.11 Validation des formulaires avec erreurs champ par champ
- Chaque champ affiche son message d'erreur specifique en rouge
- Style du champ change (bordure rouge, fond leger rouge)
- Validations : champs obligatoires, formats (heure HH:mm), longueur minimale, URLs valides

### 5.12 Interface role-based (RBAC)
- Detiction automatique du role (ENTREPRENEUR vs MENTOR) via `SessionService`
- Onglet "Mes disponibilites" visible uniquement pour les mentors
- Vue demandes : entrepreneur voit ses demandes, mentor voit les demandes recues
- Actions contextuelles : accepter/rejeter (mentor), annuler (entrepreneur)

### 5.13 Recherche et filtrage sur toutes les vues
- Champ de recherche sur chaque TableView et liste
- Filtrage en temps reel (FilteredList avec predicat dynamique)
- Recherche multi-champs : nom, projet, statut, lien

### 5.14 Tri par statut et date
- ComboBox de filtrage par statut dans la liste des demandes
- Filtre par date dans les sessions (DatePicker)

### 5.15 Toast notifications animees
- Messages de succes affichés en haut (fond vert, texte blanc)
- Animation : fade-in + slide-in (200ms), maintien 2s, fade-out (300ms)
- Superposition sur le contenu (StackPane)

### 5.16 Dialogues de confirmation
- Confirmation avant suppression avec message personnalise
- Boutons stylises "Supprimer" / "Annuler"
- Avertissement "Cette action est irreversible"

### Resume des fonctionnalites avancees (16 total)

| # | Fonctionnalite | Complexite |
|---|---------------|------------|
| 1 | Chatbot IA interactif | Haute |
| 2 | Speech-to-Text (dictee vocale) | Haute |
| 3 | Export Excel + analyse IA (Groq LLaMA) | Haute |
| 4 | Export PDF avec apercu in-app | Moyenne |
| 5 | Notifications Email + SMS automatiques | Haute |
| 6 | Dashboard avec PieChart + BarChart | Moyenne |
| 7 | Feedback et notation bidirectionnels | Moyenne |
| 8 | Auto-creation de session | Moyenne |
| 9 | Filtrage dynamique des mentors (disponibilite) | Moyenne |
| 10 | Detection de chevauchement disponibilites | Moyenne |
| 11 | Validation formulaires champ par champ | Standard |
| 12 | Interface role-based (RBAC) | Moyenne |
| 13 | Recherche/filtrage temps reel | Standard |
| 14 | Tri par statut et date | Standard |
| 15 | Toast notifications animees | Standard |
| 16 | Dialogues de confirmation | Standard |

---

## 6. Interfaces graphiques

### 6.1 Front-Office (FrontMentoratController)

**Design :**
- Interface a onglets (TabPane simule avec boutons styles)
- Design card-based moderne avec ombres portees et coins arrondis
- Palette de couleurs coherente (vert #10b981 pour mentors, orange #e67e22 pour accents)
- Police "Segoe UI" pour un rendu professionnel
- Animations de transition entre onglets (FadeTransition 200ms)

**4 onglets :**

| Onglet | Contenu | Role |
|--------|---------|------|
| Explorer les mentors | Grille de cartes mentor avec filtres date/heure + dialogue de demande riche | Tous |
| Mes demandes | Cartes de demandes avec stats, badges statut, actions contextuelles | Tous |
| Mes sessions | Sessions a venir + historique, cartes avec details | Tous |
| Mes disponibilites | CRUD disponibilites du mentor connecte | Mentor uniquement |

### 6.2 Back-Office Admin (MentoratAdminController)

**Design :**
- Dashboard professionnel avec cartes statistiques animees
- Graphiques interactifs : PieChart + BarChart
- Tables stylisees avec colonnes redimensionnables
- Formulaires Dialog avec validation visuelle
- Boutons d'action colores (bleu=modifier, rouge=supprimer, orange=ajouter)

**4 onglets :**

| Onglet | Contenu |
|--------|---------|
| Tableau de Bord | 5 stat cards + PieChart repartition demandes + BarChart sessions/mois + activites recentes |
| Demandes | TableView CRUD avec recherche, filtrage, modification statut, suppression |
| Sessions | TableView CRUD avec filtrage, ajout, modification, export PDF/Excel |
| Disponibilites | TableView CRUD avec detection chevauchement, recherche |

### 6.3 Chatbot (ChatbotController)

- Interface de chat simple (TextArea + TextField + Button)
- Prefixes "You:" et "Bot:" pour distinguer les messages
- Champ desactive pendant le chargement de la reponse IA

### 6.4 CSS et styles

- **mentorat.css** : Styles dedies au module mentorat
- **najahni-master.css** : Styles globaux de l'application (4250+ lignes)
- Gradients lineaires pour les headers
- Ombres portees (dropshadow) pour la profondeur

---

## 7. Scenarios de test utilisateur

### Scenario 1 : Entrepreneur explore et demande un mentorat

**Prerequis :** Utilisateur connecte avec le role ENTREPRENEUR

| Etape | Action | Resultat attendu |
|-------|--------|------------------|
| 1 | Naviguer vers la page Mentorat (front-office) | L'onglet "Explorer les mentors" s'affiche avec la grille des mentors disponibles |
| 2 | Selectionner une date dans le DatePicker et saisir une heure (ex: 14:00) | Le filtre se prepare |
| 3 | Cliquer sur "Rechercher" | Seuls les mentors disponibles a cette date/heure s'affichent |
| 4 | Cliquer sur "Demander un mentorat" sur la carte d'un mentor | Un dialogue de demande riche s'ouvre avec le nom du mentor en header |
| 5 | Remplir la date, l'heure, selectionner un projet dans le combobox | Les champs se mettent a jour |
| 6 | Cliquer sur le bouton micro a cote du champ "Motivation" | Le bouton passe en rouge "Stop", l'enregistrement commence |
| 7 | Dicter sa motivation puis cliquer a nouveau sur le bouton | Le texte dicte est transcrit et ajoute dans le champ via Whisper API |
| 8 | Remplir le champ "Objectifs" (manuellement ou en dictant) | Le champ est rempli |
| 9 | Cliquer "Envoyer la demande" | La demande est creee, un toast vert s'affiche, bascule sur l'onglet "Mes demandes" |
| 10 | Verifier l'email/SMS | Le mentor recoit un email HTML style + un SMS l'informant de la nouvelle demande |

### Scenario 2 : Mentor gere ses disponibilites et repond a une demande

**Prerequis :** Utilisateur connecte avec le role MENTOR

| Etape | Action | Resultat attendu |
|-------|--------|------------------|
| 1 | Naviguer vers "Mes disponibilites" | La liste des creneaux existants s'affiche |
| 2 | Cliquer "Ajouter un creneau", remplir date + heure debut/fin | Le formulaire s'ouvre |
| 3 | Saisir un creneau qui chevauche un existant | Message d'erreur "Chevauchement avec un creneau existant" |
| 4 | Corriger les heures et soumettre | Le creneau est ajoute, toast de confirmation |
| 5 | Basculer sur l'onglet "Mes demandes" | Les demandes recues s'affichent avec badges de statut |
| 6 | Cliquer "Accepter" sur une demande en attente | La demande passe en "auto_accepted", une session est automatiquement creee |
| 7 | Verifier l'email/SMS | L'entrepreneur recoit un email + SMS de confirmation d'acceptation |
| 8 | Basculer sur "Mes sessions" | La nouvelle session apparait dans "Sessions a venir" |

### Scenario 3 : Admin consulte le dashboard et gere les entites

**Prerequis :** Utilisateur connecte avec le role ADMIN

| Etape | Action | Resultat attendu |
|-------|--------|------------------|
| 1 | Naviguer vers le back-office Mentorat | Le dashboard s'affiche avec les 5 stat cards animees |
| 2 | Observer les graphiques | PieChart repartition des demandes + BarChart sessions/mois s'affichent |
| 3 | Basculer sur l'onglet "Demandes" | TableView avec toutes les demandes, barre de recherche |
| 4 | Taper "pending" dans la recherche | Seules les demandes en attente s'affichent |
| 5 | Cliquer "Modifier" sur une demande | Dialogue d'edition avec ComboBox de statut |
| 6 | Changer le statut de "pending_review" a "rejected" | La demande est mise a jour, notification email + SMS envoyee a l'entrepreneur |
| 7 | Basculer sur "Sessions" | TableView des sessions |
| 8 | Cliquer "Modifier" sur une session, changer le statut a "completed" | La session est mise a jour |
| 9 | Cliquer "Supprimer" sur une session | Dialogue de confirmation "Cette action est irreversible" |
| 10 | Confirmer la suppression | La session est supprimee, toast de confirmation |

### Scenario 4 : Export PDF et Excel avec analyse IA

**Prerequis :** Etre sur la page de liste des sessions (MentorshipSessionList)

| Etape | Action | Resultat attendu |
|-------|--------|------------------|
| 1 | Cliquer sur le bouton "Export PDF" | Un PDF A4 paysage est genere avec un tableau style de toutes les sessions |
| 2 | Observer l'apercu in-app | Le PDF s'affiche dans un popup integre (PDFPreviewPopup) |
| 3 | Fermer l'apercu | Retour a la liste des sessions |
| 4 | Cliquer sur le bouton "Export Excel" | Un dialogue FileChooser s'ouvre pour choisir l'emplacement |
| 5 | Selectionner un emplacement et confirmer | Le bouton affiche "Exporting...", appel a l'API Groq IA en arriere-plan |
| 6 | Attendre la fin de l'export | Le fichier .xlsx est genere avec 2 feuilles |
| 7 | Ouvrir le fichier dans Excel | Feuille "Sessions" : donnees formatees. Feuille "AI Insights" : statistiques + analyse IA |

### Scenario 5 : Utilisation du chatbot IA

**Prerequis :** Acces a l'interface Chatbot depuis la liste des demandes

| Etape | Action | Resultat attendu |
|-------|--------|------------------|
| 1 | Cliquer sur le bouton Chatbot | L'interface de chat s'ouvre |
| 2 | Taper "Comment trouver un bon mentor ?" | Le message "You: ..." apparait dans le chat |
| 3 | Attendre la reponse | Le champ est desactive pendant le chargement. "Bot: ..." apparait avec la reponse IA |
| 4 | Poser une autre question | L'historique se met a jour avec la nouvelle question et reponse |

---

## 8. Donnees de test

### 8.1 Utilisateurs de test

| ID | Prenom | Nom | Role | Email | Telephone |
|----|--------|-----|------|-------|-----------|
| 1 | Ilyes | Guesmi | ADMIN | ilyessguesmi7@gmail.com | +21612345678 |
| 2 | Ahmed | Ben Ali | ENTREPRENEUR | ahmed.benali@test.com | +21698765432 |
| 3 | Sarra | Mansouri | MENTOR | sarra.mansouri@test.com | +21655443322 |
| 4 | Mohamed | Trabelsi | MENTOR | mohamed.trabelsi@test.com | +21699887766 |
| 5 | Fatma | Hamdi | ENTREPRENEUR | fatma.hamdi@test.com | +21677889900 |

### 8.2 Disponibilites test

| ID | Mentor | Date | Debut | Fin |
|----|--------|------|-------|-----|
| 1 | Sarra Mansouri (3) | 2025-02-15 | 09:00 | 12:00 |
| 2 | Sarra Mansouri (3) | 2025-02-16 | 14:00 | 17:00 |
| 3 | Mohamed Trabelsi (4) | 2025-02-15 | 10:00 | 13:00 |
| 4 | Mohamed Trabelsi (4) | 2025-02-17 | 08:00 | 11:00 |

### 8.3 Demandes test

| ID | Entrepreneur | Mentor | Date | Heure | Statut | Motivation |
|----|-------------|--------|------|-------|--------|------------|
| 1 | Ahmed (2) | Sarra (3) | 2025-02-15 | 10:00 | auto_accepted | Besoin d'aide pour mon business plan fintech |
| 2 | Fatma (5) | Mohamed (4) | 2025-02-15 | 11:00 | pending_review | Je souhaite un accompagnement en strategie |
| 3 | Ahmed (2) | Mohamed (4) | 2025-02-17 | 09:00 | rejected | Demande de conseil en marketing digital |
| 4 | Fatma (5) | Sarra (3) | 2025-02-16 | 15:00 | completed | Aide pour lever des fonds |

### 8.4 Sessions test

| ID | Request | Date/Heure | Duree | Statut | Note Mentor | Note Entrepreneur |
|----|---------|------------|-------|--------|-------------|-------------------|
| 1 | 1 | 2025-02-15 10:00 | 60 min | completed | 5 | 4 |
| 2 | 4 | 2025-02-16 15:00 | 60 min | scheduled | - | - |

### 8.5 SQL d'insertion des donnees de test

```sql
-- Disponibilites
INSERT INTO mentor_availability (mentor_id, date, start_time, end_time, created_at)
VALUES
(3, '2025-02-15', '09:00:00', '12:00:00', NOW()),
(3, '2025-02-16', '14:00:00', '17:00:00', NOW()),
(4, '2025-02-15', '10:00:00', '13:00:00', NOW()),
(4, '2025-02-17', '08:00:00', '11:00:00', NOW());

-- Demandes
INSERT INTO mentorship_request (entrepreneur_id, mentor_id, project_id, date, time, motivation, goals, match_score, auto_approved, status, created_at)
VALUES
(2, 3, 1, '2025-02-15', '10:00', 'Besoin d aide pour mon business plan fintech', 'Valider mon modele economique', 0.85, 1, 'auto_accepted', NOW()),
(5, 4, 2, '2025-02-15', '11:00', 'Je souhaite un accompagnement en strategie', 'Developper ma strategie de croissance', 0.72, 0, 'pending_review', NOW()),
(2, 4, 1, '2025-02-17', '09:00', 'Demande de conseil en marketing digital', 'Lancer une campagne marketing', 0.60, 0, 'rejected', NOW()),
(5, 3, 2, '2025-02-16', '15:00', 'Aide pour lever des fonds', 'Preparer un pitch investisseur', 0.90, 0, 'completed', NOW());

-- Sessions
INSERT INTO mentorship_session (request_id, scheduled_at, duration_minutes, status, mentor_feedback, entrepreneur_feedback, mentor_rating, entrepreneur_rating, created_at)
VALUES
(1, '2025-02-15 10:00:00', 60, 'completed', 'Entrepreneur tres motive, bonne comprehension du marche', 'Mentor tres professionnel et utile', 5, 4, NOW()),
(4, '2025-02-16 15:00:00', 60, 'scheduled', NULL, NULL, 0, 0, NOW());
```

---

## 9. Concepts theoriques appliques

### 9.1 Patterns de conception

| Pattern | Application |
|---------|-------------|
| **Singleton** | `MyConnection` (connexion JDBC unique), `SessionService` (utilisateur connecte) |
| **MVC** | Separation claire Models/Views(FXML)/Controllers |
| **Service Layer** | Services metier distincts (ServiceMentorshipRequest, etc.) |
| **Observer** | `FilteredList` avec predicats dynamiques pour recherche temps reel |
| **Strategy** | Validation conditionnelle selon le statut et le role |
| **Template Method** | Methodes utilitaires partagees (buildStatCard, createStyledTable, etc.) |

### 9.2 Programmation asynchrone

- **Threads nommees** pour les envois email/SMS (ne bloquent pas l'UI JavaFX)
- **Platform.runLater()** pour mettre a jour l'UI depuis les threads secondaires (Chatbot, export)
- **Background threads** pour l'appel a l'API Groq AI (generation insights Excel)

### 9.3 Securite et validation

| Aspect | Implementation |
|--------|---------------|
| RBAC | Role-based access control sur les onglets et actions |
| Ownership | Seul le proprietaire peut modifier/supprimer ses disponibilites |
| Input validation | Validation cote client exhaustive (format, longueur, obligatoire) |
| SQL Injection | PreparedStatements systematiques |
| Credentials | Fichier `secrets.properties` externe (non commite) |

### 9.4 Concepts JavaFX avances

| Concept | Utilisation |
|---------|-------------|
| **Animations** | FadeTransition, TranslateTransition, ParallelTransition, PauseTransition |
| **Charts** | PieChart (repartition statuts), BarChart (sessions/mois) |
| **Custom Cells** | TableCell personnalisees avec styles de statut colores |
| **Dynamic UI** | Construction programmatique des vues (pas de FXML pour le contenu dynamique) |
| **CSS Styling** | Styles inline + classes CSS dediees, hover effects |
| **Dialog API** | Dialog<ButtonType> avec validation EventFilter |

### 9.5 Integration d'APIs

| Concept | Implementation |
|---------|---------------|
| **REST Client** | HttpURLConnection pour Groq AI, FastAPI |
| **Multipart Upload** | Construction manuelle des requetes multipart pour Whisper |
| **SMTP** | Jakarta Mail avec authentification TLS |
| **SDK Integration** | Twilio Java SDK pour SMS |
| **JSON Parsing** | Parsing natif (sans librairie externe) |
| **Error Handling** | Try-catch systematique avec logs et fallback gracieux |

---

## Conclusion

Le module Mentorat de NAJAHNI represente une implementation complete et professionnelle
d'un systeme de gestion de mentorat. Avec **8 APIs/services externes integres**,
**16 fonctionnalites avancees**, un **front-office et back-office entierement fonctionnels**,
des **notifications temps reel** (email + SMS), et une **analyse IA** des donnees,
le module depasse largement les criteres attendus pour une validation Sprint JavaFX.
