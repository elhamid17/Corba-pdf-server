# RAPPORT DE PROJET

## CORBA PDF Suite

### Plateforme distribuée de traitement de documents PDF

---

**Université** : Université du Sine Saloum El Hadji Ibrahima NIASS (USSEIN)
**Filière** : L2 AgroTIC
**Année universitaire** : 2025 - 2026
**Auteur** : Elhamid NDAO
**Email** : eahamid.ndao@etu.ussein.edu.sn
**Encadrant** : [À compléter]
**Date de soutenance** : [À compléter]

---

**Dépôt du code source** : https://github.com/elhamid17/Corba-pdf-server
**Application en production** : https://corba-pdf.onrender.com
**API REST en production** : https://corba-pdf-api.onrender.com

---

## Sommaire

1. [Introduction](#1-introduction)
2. [État de l'art et contexte technique](#2-état-de-lart-et-contexte-technique)
3. [Conception architecturale](#3-conception-architecturale)
4. [Modèle d'interface CORBA (IDL)](#4-modèle-dinterface-corba-idl)
5. [Implémentation du serveur CORBA](#5-implémentation-du-serveur-corba)
6. [Implémentation de la passerelle REST](#6-implémentation-de-la-passerelle-rest)
7. [Implémentation du frontend React](#7-implémentation-du-frontend-react)
8. [Conteneurisation et orchestration locale](#8-conteneurisation-et-orchestration-locale)
9. [Déploiement en production sur Render](#9-déploiement-en-production-sur-render)
10. [Tests et validation](#10-tests-et-validation)
11. [Difficultés rencontrées et solutions](#11-difficultés-rencontrées-et-solutions)
12. [Conclusion et perspectives](#12-conclusion-et-perspectives)
13. [Annexes](#13-annexes)

---

## 1. Introduction

### 1.1 Contexte

La manipulation programmatique de documents PDF (fusion, découpage, signature, OCR, compression, etc.) est un besoin transversal à de nombreux systèmes d'information. Plutôt que d'embarquer la logique de traitement dans chaque application cliente, l'approche distribuée consiste à centraliser ces opérations sur un serveur dédié exposé via un protocole standardisé.

CORBA (Common Object Request Broker Architecture), spécification de l'OMG (Object Management Group), permet la communication entre objets distants indépendamment du langage et de la plateforme grâce au protocole IIOP (Internet Inter-ORB Protocol). Même si CORBA n'est plus le standard dominant dans l'industrie au profit de gRPC, REST ou GraphQL, il reste un excellent support pédagogique pour comprendre les fondements des architectures distribuées : description d'interface (IDL), génération de stubs et squelettes, courtage de requêtes (ORB), activation d'objets (POA), service de noms.

### 1.2 Problématique

Comment construire une plateforme distribuée et hétérogène où :
- un cœur de traitement PDF en Java expose ses services via CORBA,
- une passerelle web traduit les appels HTTP/REST en appels CORBA,
- une interface utilisateur web moderne consomme ces services,
- l'ensemble s'orchestre via Docker et se déploie en production sur un hébergeur gratuit ?

### 1.3 Objectifs

1. **Concevoir** une interface CORBA exhaustive (IDL) couvrant 14 opérations PDF.
2. **Implémenter** un serveur Java/JacORB qui réalise ces opérations grâce à Apache PDFBox, Tess4J et BouncyCastle.
3. **Développer** une passerelle Spring Boot 3 exposant l'API en REST/JSON.
4. **Construire** une interface utilisateur React de qualité entreprise.
5. **Orchestrer** la solution avec Docker Compose et Nginx.
6. **Déployer** la suite en ligne sur un hébergeur gratuit avec HTTPS.

### 1.4 Plan du rapport

Le rapport suit la chaîne logique de construction du projet : étude de l'existant, conception, implémentation des trois couches (CORBA, REST, web), industrialisation par conteneurs, mise en production et validation.

---

## 2. État de l'art et contexte technique

### 2.1 CORBA et JacORB

**CORBA** est une norme datant de 1991 qui définit :
- l'**IDL** (Interface Definition Language), syntaxe neutre décrivant les contrats,
- le **GIOP** (General Inter-ORB Protocol) et sa déclinaison TCP **IIOP**,
- l'**ORB** (Object Request Broker), middleware qui localise et invoque les objets,
- le **POA** (Portable Object Adapter), gestionnaire de cycle de vie côté serveur,
- des services standardisés : Naming, Trading, Event, Notification, etc.

**JacORB** est l'implémentation Java open-source de référence pour CORBA, maintenue depuis 1997. Elle fournit l'ORB, le compilateur IDL→Java et l'ensemble des services standard. Le projet utilise la version 3.9 pour sa compatibilité éprouvée avec Java 17.

### 2.2 Apache PDFBox

Bibliothèque Java sous licence Apache 2.0 pour la création, la lecture et la manipulation de documents PDF. Version utilisée : 2.0.31. Elle apporte :
- l'API `PDDocument` pour parcourir les pages,
- `PDFMergerUtility` pour la fusion,
- `PDFRenderer` pour la conversion en images,
- `PDFTextStripper` pour l'extraction de texte,
- des protections (chiffrement AES, mots de passe propriétaire/utilisateur).

### 2.3 Tess4J

Binding Java de Tesseract OCR via JNA. Permet la reconnaissance optique de caractères sur des PDF scannés. Le projet supporte le français et l'anglais (`tesseract-ocr-data-fra` et `tesseract-ocr-data-eng`).

### 2.4 BouncyCastle

Suite cryptographique Java pour la signature numérique des PDF via certificats PKCS#12. Versions BCprov et BCpkix 1.78.1.

### 2.5 Spring Boot 3

Framework Java pour applications web auto-portées. La version 3.2.5 exige Java 17 minimum et apporte le starter `web` (Tomcat embarqué + Jackson + Spring MVC) ainsi que `actuator` pour les sondes de santé.

### 2.6 React 18 + Vite + Tailwind

Stack frontend moderne : React 18 pour le rendu déclaratif, Vite 5 comme bundler ultra-rapide en développement, Tailwind CSS 3 pour le design utilitaire. Lucide-react fournit l'iconographie, react-router-dom 6 la navigation client.

---

## 3. Conception architecturale

### 3.1 Architecture en couches

La plateforme suit une architecture distribuée en trois tiers, chaque tier étant lui-même conteneurisé :

```
+----------------------------------------------------+
|                 NAVIGATEUR (CLIENT)                |
+----------------------------------------------------+
                         | HTTPS
                         v
+----------------------------------------------------+
|     NGINX  (reverse proxy + TLS)                   |
|     route /        -> frontend (port 80)           |
|     route /api/    -> api-gateway (port 8080)      |
+----------------------------------------------------+
              |                       |
              v                       v
   +---------------------+   +-----------------------+
   |  Frontend React     |   |  API Gateway          |
   |  Nginx + bundle JS  |   |  Spring Boot 3.2.5    |
   |  port 80            |   |  port 8080            |
   +---------------------+   +-----------------------+
                                       | IIOP (CORBA)
                                       v
                             +-----------------------+
                             |  Serveur CORBA        |
                             |  JacORB 3.9           |
                             |  PDFBox + Tess4J      |
                             |  port 2809            |
                             +-----------------------+
```

### 3.2 Justification des choix

| Choix | Justification |
|---|---|
| Découplage CORBA/REST | La passerelle permet à n'importe quel client web d'utiliser les services sans implémenter d'ORB. |
| Java 17 (et non 11) | Spring Boot 3.x impose Java 17 minimum. L'alignement complet évite les surprises runtime. |
| Multi-module Maven | Isole les contrats (corba-idl), le serveur, la passerelle. Permet une réutilisation des stubs. |
| Tailwind CSS | Productivité élevée, design system cohérent sans CSS éparpillé. |
| Nginx reverse proxy | Point d'entrée unique, gestion des en-têtes HTTPS, agrégation des upstreams. |
| Réseaux Docker isolés | Le CORBA n'est jamais exposé sur internet : il vit dans un réseau `backend` interne. |

### 3.3 Diagramme de séquence type — création d'un PDF

```
Navigateur          Nginx        Gateway        CORBA Server      PDFBox
   |                  |              |                |              |
   |---POST /api/pdf/create--------->|                |              |
   |                  |              |                |              |
   |                  |    createFromText(text,title)  |              |
   |                  |              |--------------->|              |
   |                  |              |                |--PDDocument->|
   |                  |              |                |<--bytes------|
   |                  |              |<--PDFResult----|              |
   |                  |              |                |              |
   |<--application/pdf (Content-Disposition: attachment)             |
```

---

## 4. Modèle d'interface CORBA (IDL)

### 4.1 Structure des fichiers IDL

Le module `corba-idl` contient deux fichiers IDL compilés en classes Java par le compilateur de JacORB :

- `Types.idl` : définit les types partagés (structures, exceptions, séquences).
- `PDFService.idl` : définit l'interface des opérations métier.

### 4.2 Types fondamentaux

```idl
typedef sequence<octet> PDFData;       // un PDF binaire
typedef sequence<PDFData> PDFList;     // une liste de PDF
typedef sequence<long> PageList;       // liste de pages

struct PDFResult {
  boolean success;
  PDFData data;
  string  message;
};

struct PDFMetadata {
  string title;     string author;   string subject;
  string keywords;  string creator;  string producer;
  string creationDate;
  long   pageCount;
};

struct CompressOptions { boolean compressImages; long imageQuality; boolean removeMetadata; };
struct WatermarkOptions { string text; float opacity; long fontSize; boolean diagonal; };
struct ConvertOptions   { string format; long dpi; };
```

### 4.3 Exceptions personnalisées

```idl
exception PDFException          { string code; string message; };
exception InvalidPageException  { long requestedPage; long totalPages; string message; };
exception PasswordException     { string message; };
```

### 4.4 Interface PDFService

L'interface définit 16 opérations regroupées en 14 services métier plus 2 utilitaires :

| # | Opération | Signature simplifiée |
|---|---|---|
| 1 | Fusion | `PDFResult merge(PDFList)` |
| 2 | Découpage | `PDFList split(PDFData, PageList)` |
| 3 | Extraction de pages | `PDFResult extractPages(PDFData, PageList)` |
| 4 | Suppression de pages | `PDFResult deletePages(PDFData, PageList)` |
| 5 | Compression | `PDFResult compress(PDFData, CompressOptions)` |
| 6 | Rotation | `PDFResult rotate(PDFData, PageList, long angle)` |
| 7 | Filigrane | `PDFResult addWatermark(PDFData, WatermarkOptions)` |
| 8 | Mot de passe | `PDFResult addPassword(PDFData, user, owner)` |
| 9 | Conversion images | `PDFList convertToImages(PDFData, ConvertOptions)` |
| 10 | Extraction texte | `string extractText(PDFData)` |
| 11 | OCR | `string performOCR(PDFData, language)` |
| 12 | Signature | `PDFResult sign(PDFData, certificate, password, reason, location)` |
| 13a | Lire métadonnées | `PDFMetadata getMetadata(PDFData)` |
| 13b | Écrire métadonnées | `PDFResult setMetadata(PDFData, PDFMetadata)` |
| 14 | Création depuis texte | `PDFResult createFromText(string text, string title)` |
| util | Compter les pages | `long getPageCount(PDFData)` |
| util | Sonde | `string ping()` |

### 4.5 Compilation des stubs

Le `pom.xml` du module `corba-idl` configure le plugin `maven-antrun-plugin` pour invoquer `org.jacorb.idl.parser` en phase `generate-sources`. Les classes générées sont produites dans `target/generated-sources/idl/` et incluses dans le classpath des autres modules via la dépendance Maven.

---

## 5. Implémentation du serveur CORBA

### 5.1 Cycle de vie de l'ORB — `ServerMain.java`

Le démarrage suit la séquence canonique CORBA :

1. Construction des propriétés ORB (`org.omg.CORBA.ORBClass=org.jacorb.orb.ORB`, port `OAPort=2809`).
2. Initialisation de l'ORB : `ORB.init(args, props)`.
3. Résolution du `RootPOA` et activation du `POAManager`.
4. Instanciation de `PDFServiceImpl` (skeleton).
5. Activation de l'objet dans le POA et conversion en référence CORBA.
6. **Persistance de l'IOR** (Interoperable Object Reference) dans `/volumes/pdf-storage/PDFService.ior` pour que la passerelle puisse résoudre l'objet sans Naming Service.
7. Tentative d'enregistrement dans le Naming Service (optionnel, fallback IOR).
8. `orb.run()` : boucle d'attente bloquante.

### 5.2 Séparation impl ↔ handlers

`PDFServiceImpl` étend la classe `PDFServicePOA` générée et délègue chaque méthode à un handler dédié. Cette séparation respecte le principe de responsabilité unique :

```
PDFServiceImpl
   ├── MergeHandler           (PDFMergerUtility)
   ├── SplitHandler           (PDDocument.removePage)
   ├── ExtractHandler         (PDDocument.addPage)
   ├── DeletePageHandler
   ├── CompressHandler        (PDImageXObject + ImageIO compression JPEG)
   ├── RotateHandler          (PDPage.setRotation)
   ├── WatermarkHandler       (PDPageContentStream texte transparent)
   ├── PasswordHandler        (StandardProtectionPolicy AES 128)
   ├── ConvertHandler         (PDFRenderer.renderImageWithDPI)
   ├── TextExtractHandler     (PDFTextStripper)
   ├── OcrHandler             (Tess4J + image render)
   ├── SignHandler            (BouncyCastle PKCS12 + PDFBox SignatureInterface)
   ├── MetadataHandler        (PDDocumentInformation)
   └── CreateHandler          (PDFont + PDPageContentStream)
```

### 5.3 Gestion des fichiers temporaires — `FileUtil.java`

Toutes les opérations qui requièrent un fichier sur disque (PDFBox `loadPDF(File)`) passent par `FileUtil.bytesToTempFile()` puis `FileUtil.deleteSilently()` dans un bloc `finally`, évitant toute fuite de fichiers dans `/tmp`.

### 5.4 Configuration JacORB

Le fichier `jacorb.properties` règle :

```
OAPort=2809
OAIAddr=0.0.0.0
ORBInitRef.NameService=corbaloc::localhost:2809/NameService
jacorb.log.default.verbosity=3
jacorb.maxManagedBufSize=1048576
jacorb.connection.client.connect_timeout=30000
jacorb.implname=PDFServer
```

---

## 6. Implémentation de la passerelle REST

### 6.1 `CorbaClientService` — connexion au serveur

Service Spring marqué `@Service`. Cycle de vie :

- `@PostConstruct connect()` :
  1. Initialise un ORB client JacORB.
  2. Tente de **lire l'IOR** depuis le volume partagé `/volumes/pdf-storage/PDFService.ior`.
  3. Fallback : résoud via le Naming Service.
  4. Narrowing : `PDFServiceHelper.narrow(obj)` → proxy fortement typé.
  5. Test : appel `ping()`.
- `@PreDestroy disconnect()` : `orb.shutdown(false)`.

La méthode `getPdfService()` tente une reconnexion à la demande si le proxy est null (résilience aux redémarrages du serveur CORBA).

### 6.2 `PDFController` — façade REST

Le contrôleur expose 16 endpoints sous `/api/pdf/*` :

| Endpoint | Méthode | Réponse |
|---|---|---|
| `/ping` | GET | JSON statut |
| `/merge` | POST multipart | PDF binaire |
| `/split` | POST multipart | ZIP de PDF |
| `/extract-pages` | POST multipart | PDF |
| `/delete-pages` | POST multipart | PDF |
| `/compress` | POST multipart | PDF |
| `/rotate` | POST multipart | PDF |
| `/watermark` | POST multipart | PDF |
| `/protect` | POST multipart | PDF |
| `/convert-to-images` | POST multipart | ZIP |
| `/extract-text` | POST multipart | JSON `{text}` |
| `/ocr` | POST multipart | JSON `{text, language}` |
| `/sign` | POST multipart | PDF |
| `/metadata` | POST multipart | JSON |
| `/create` | POST form | PDF |
| `/page-count` | POST multipart | JSON `{pageCount}` |

### 6.3 Validation et gestion d'erreur

Chaque endpoint valide ses entrées (fichier non vide, plages valides, paramètres positifs). Les erreurs sont propagées via `ResponseStatusException` puis interceptées par deux handlers :

- `@ExceptionHandler(ResponseStatusException.class)` : conserve le code HTTP, formate un corps JSON `{error, message, status, timestamp}`.
- `@ExceptionHandler(Exception.class)` : 500 générique, journalise la stack.

### 6.4 Sérialisation des réponses binaires

Deux helpers internes :

- `pdfResponse(PDFResult, filename)` : produit un `ResponseEntity<byte[]>` avec `Content-Type: application/pdf` et `Content-Disposition: attachment`.
- `zipResponse(byte[][], zipName, prefix, ext)` : construit un ZIP en mémoire avec `ZipOutputStream` pour les opérations multi-résultats (split, convert).

### 6.5 Configuration CORS

`CorsConfig.java` lit la liste blanche depuis la propriété `app.cors.allowed-origins` (binding sur `CORS_ALLOWED_ORIGINS`). Cela permet d'autoriser dynamiquement le frontend (local en dev, Render en prod) sans modifier le code.

---

## 7. Implémentation du frontend React

### 7.1 Stack et organisation

- **React 18.3** + **Vite 5.4** (bundler) + **TypeScript** non utilisé (JSX pur, plus simple à lire).
- **Tailwind CSS 3.4** pour le design utilitaire.
- **react-router-dom 6** pour le routage SPA.
- **react-dropzone 14** pour les zones de dépôt de fichiers.
- **lucide-react 0.460** pour les icônes (16+ utilisées).

Structure :

```
frontend/src/
├── api/pdfApi.js          # couche d'accès à l'API REST
├── components/
│   ├── Navbar.jsx         # topbar sticky + status pill temps réel
│   ├── Footer.jsx
│   ├── DropZone.jsx       # multi-fichiers, suppression individuelle
│   ├── ServiceCard.jsx    # carte de service (icône, titre, badge)
│   ├── SubmitButton.jsx   # bouton CTA + spinner intégré
│   ├── ToolPage.jsx       # layout standard des pages outils
│   └── Toast.jsx          # provider de notifications globales
├── pages/                 # 14 pages outils + Home + NotFound
└── index.css              # design system (tokens, classes utilitaires)
```

### 7.2 Design system

Tokens définis dans `tailwind.config.js` :

- **brand** (indigo 50→950) : couleur primaire.
- **accent** (cyan 400→600) : compléments CTA.
- **ink** (slate 50→950) : nuances de gris pour le texte et les fonds.
- **Polices** : Inter (UI), Plus Jakarta Sans (titres), JetBrains Mono (code).
- **Ombres custom** : `card`, `cardHover`, `glow`.
- **Animations** : `fade-in`, `slide-up`, `pulse-dot`, `shimmer`.

### 7.3 Classes utilitaires personnalisées

Dans `index.css`, sous `@layer components` :

```
.btn-primary       /* bouton CTA principal */
.btn-secondary     /* bouton secondaire */
.btn-ghost         /* bouton transparent */
.field             /* input/textarea standardisé */
.field-label       /* label de formulaire */
.card / .card-hover
.badge-success / .badge-error / .badge-warn / .badge-info
.alert-success / .alert-error / .alert-info
.eyebrow           /* en-tête de section discret */
```

### 7.4 Composants clés

- **`Navbar`** : sticky top avec backdrop-blur, logo en gradient, navigation principale visible en ≥ lg, navigation secondaire scrollable en mobile, **pill statut serveur en temps réel** (ping toutes les pages).
- **`DropZone`** : intègre `react-dropzone`, gère le multi-fichiers, affiche la liste avec taille humaine (Ko/Mo) et bouton de suppression par fichier, états visuels distincts pour drag-active et drag-reject.
- **`ToolPage`** : header en gradient radial + grille discrète, fil d'Ariane, container `max-w-3xl` centré, carte blanche avec ombre.
- **`Toast`** : contexte React global, file de toasts auto-dismiss (4-6 s), API `useToast()` exposée aux pages.

### 7.5 Responsivité

5 breakpoints Tailwind utilisés (sm 640, md 768, lg 1024, xl 1280, 2xl 1536). Exemples :

- Hero `text-4xl sm:text-5xl lg:text-6xl`.
- Grille services `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4`.
- Topbar : liens principaux `hidden lg:flex`, liens secondaires `lg:hidden` scroll horizontal.
- StatusPill : libellé `hidden sm:inline` (seul le point reste visible sur mobile très étroit).

### 7.6 Couche API

`pdfApi.js` centralise tous les appels. Méthode `postForm()` factorisée, lecture intelligente d'erreur (JSON ou texte). Base URL configurable via `import.meta.env.VITE_API_URL` pour permettre le déploiement cross-origin sans toucher au code.

---

## 8. Conteneurisation et orchestration locale

### 8.1 Dockerfiles

| Image | Base | Particularités |
|---|---|---|
| corba-server | eclipse-temurin:17-jre-alpine | + tesseract-ocr (fra/eng) + fontconfig + ttf-dejavu + netcat-openbsd |
| api-gateway | eclipse-temurin:17-jre-alpine | jar Spring Boot exécutable |
| frontend | node:20-alpine (build) → nginx:1.25-alpine (runtime) | multi-stage, sert le bundle Vite |

### 8.2 `docker-compose.yml`

Quatre services orchestrés :

```
nginx (public 80/443)
  ├── frontend     (réseau frontend)
  └── api-gateway  (réseau frontend ∩ backend)
        └── corba-server  (réseau backend isolé, jamais exposé)
```

Volumes :
- `pdf-storage` : bind sur `./volumes/pdf-storage/` — partage l'IOR entre serveur et passerelle.
- `tessdata` : bind sur `./volumes/tessdata/` — données Tesseract.

Réseaux :
- `frontend` : pont accessible.
- `backend` : `internal: true` — aucune sortie internet, isole le CORBA.

Healthchecks :
- corba-server : `nc -z localhost 2809`
- api-gateway : `wget -qO- http://localhost:8080/api/pdf/ping`

### 8.3 Reverse proxy Nginx

`nginx/nginx.conf` route :
- `/api/` → `http://api-gateway:8080/api/` avec en-têtes `X-Real-IP`, `X-Forwarded-*` et timeouts longs (300 s) pour les gros PDF.
- `/health` → `/actuator/health`.
- `/` → `http://frontend:80/` avec fallback `/index.html` pour le routage SPA.

### 8.4 Scripts

- **`build.sh`** : `mvn clean package` puis `docker compose up --build -d`.
- **`deploy.sh`** : `git pull` puis `docker compose down && up --build -d`.

---

## 9. Déploiement en production sur Render

### 9.1 Contraintes du free tier

- 1 seul service web Docker gratuit, 512 Mo de RAM.
- Sleep après 15 min d'inactivité (réveil ~30 s).
- HTTPS automatique (Let's Encrypt managé).
- Build et déploiement automatiques depuis GitHub.

### 9.2 Stratégie d'adaptation

Trois ajustements ont été nécessaires :

1. **Fusion des deux process Java** (serveur CORBA + passerelle) dans un seul conteneur via `supervisord`. Communication CORBA via `localhost:2809`, partage de l'IOR dans `/volumes/pdf-storage/` interne au conteneur.
2. **Réduction des heaps JVM** :
   - corba-server : `-Xmx180m -Xss256k -XX:+UseSerialGC`
   - api-gateway : `-Xmx240m -Xss256k -XX:+UseSerialGC`
3. **Frontend en site statique séparé** (gratuit, illimité, CDN, pas de cold start).

### 9.3 Fichiers ajoutés au dépôt

- **`Dockerfile.render`** : image multi-stage qui compile en Maven puis emballe les deux jars avec Tesseract et supervisord.
- **`supervisord.conf`** : déclare deux programmes (`corba-server` priorité 10, `api-gateway` priorité 20 démarrage différé de 20 s).
- **`render.yaml`** : blueprint qui décrit les deux services Render (Docker + static).

### 9.4 Variables d'environnement

| Variable | Service | Valeur |
|---|---|---|
| `PORT` | backend | `8080` (injecté par Render) |
| `CORBA_HOST` | backend | `localhost` |
| `CORBA_PORT` | backend | `2809` |
| `CORS_ALLOWED_ORIGINS` | backend | `https://corba-pdf.onrender.com` |
| `JAVA_OPTS_CORBA` | backend | heap 180 Mo + GC série |
| `JAVA_OPTS_GATEWAY` | backend | heap 240 Mo + GC série |
| `VITE_API_URL` | frontend | `https://corba-pdf-api.onrender.com/api/pdf` |

### 9.5 Modifications de code rendues nécessaires

- `application.yml` : `server.port: ${PORT:8080}` + `forward-headers-strategy: framework` (HTTPS derrière proxy).
- `pdfApi.js` : `const BASE = (import.meta.env.VITE_API_URL || '/api/pdf').replace(/\/+$/, '')` pour basculer entre dev (proxy local) et prod (cross-origin).

### 9.6 URLs publiques

- **Frontend** : https://corba-pdf.onrender.com
- **API** : https://corba-pdf-api.onrender.com/api/pdf/...
- **Sonde santé** : https://corba-pdf-api.onrender.com/actuator/health

### 9.7 CI/CD implicite

Render observe la branche `main` du repo GitHub. Chaque `git push` déclenche :
1. clone du repo,
2. build Maven dans le conteneur,
3. assemblage de l'image runtime,
4. déploiement bleu-vert avec bascule automatique.

---

## 10. Tests et validation

### 10.1 Smoke tests locaux

Réalisés après chaque build via `curl` directement vers les conteneurs locaux :

| Endpoint | Méthode | Résultat attendu | Résultat obtenu |
|---|---|---|---|
| `/api/pdf/ping` | GET | `{"status":"OK", "server":"..."}` | OK |
| `/actuator/health` | GET | `{"status":"UP", ...}` | OK |
| `/api/pdf/create` | POST | PDF v1.4 binaire | PDF 996 octets valide |
| `/api/pdf/page-count` | POST | `{"pageCount":1}` | OK |
| `/api/pdf/metadata` | POST | JSON métadonnées | OK |
| Frontend `GET /` | GET | HTTP 200 + HTML | OK |

### 10.2 Tests en production

Mêmes endpoints, mêmes résultats sur les URLs Render :

```
$ curl https://corba-pdf-api.onrender.com/api/pdf/ping
{"status":"OK","server":"CORBA PDF Server — OK — 2026-05-17T01:41:15..."}

$ curl https://corba-pdf-api.onrender.com/actuator/health
{"status":"UP","components":{"diskSpace":{"status":"UP",...},
 "livenessState":{"status":"UP"},"ping":{"status":"UP"},
 "readinessState":{"status":"UP"}}}

$ curl -X POST -F "text=Hello Render" -F "title=Demo" \
       https://corba-pdf-api.onrender.com/api/pdf/create -o /tmp/r.pdf
$ file /tmp/r.pdf
/tmp/r.pdf: PDF document, version 1.4, 1 page(s)
```

### 10.3 Tests d'interface

Accès via navigateur à https://corba-pdf.onrender.com :
- Pastille statut passe à "Opérationnel" en ~30 s (réveil cold start).
- Navigation entre les 14 pages outils sans rechargement (SPA).
- Drag & drop de fichiers PDF fonctionnel.
- Toasts d'erreur affichés correctement lors de validations bloquantes.
- Responsivité validée sur mobile (375 px), tablette (768 px), desktop (1280 px+).

---

## 11. Difficultés rencontrées et solutions

### 11.1 Incompatibilité Java 11 / Spring Boot 3

**Symptôme** : Le `pom.xml` parent fixait Java 11 mais Spring Boot 3.2.5 exige Java 17. Le `Dockerfile` de la passerelle utilisait déjà JDK 17, créant une dissonance entre bytecode et runtime.

**Solution** : Bump à Java 17 dans le `pom.xml` parent (`maven.compiler.source/target`) et alignement du `Dockerfile` du serveur CORBA sur `eclipse-temurin:17-jre-alpine`.

### 11.2 Erreur 500 sur tous les endpoints `@RequestParam`

**Symptôme** : Au premier déploiement, `POST /api/pdf/create` retournait 500 avec :
> `IllegalArgumentException: Name for argument of type [java.lang.String] not specified, and parameter name information not available via reflection. Ensure that the compiler uses the '-parameters' flag.`

**Solution** : Activer le drapeau `-parameters` du compilateur Java dans le `pom.xml` parent :

```xml
<configuration>
  <source>17</source>
  <target>17</target>
  <parameters>true</parameters>
</configuration>
```

### 11.3 Endpoint `/actuator/health` introuvable

**Symptôme** : Le `pom.xml` de la passerelle déclarait `spring-boot-actuator` (artefact d'autoconfiguration seul), pas `spring-boot-starter-actuator`. Les endpoints d'actuator n'étaient donc pas activés.

**Solution** : Remplacement par `spring-boot-starter-actuator`.

### 11.4 Conflit Tailwind v4 ↔ syntaxe v3

**Symptôme** : Le `package.json` initial déclarait Tailwind 4.3 et `@tailwindcss/postcss`, mais `index.css` utilisait `@tailwind base;` (syntaxe v3) et le `tailwind.config.js` exportait à la mode v3. Le build cassait.

**Solution** : Alignement complet sur Tailwind 3.4 (stable, syntaxe v3) avec `postcss.config.js` pointant sur `tailwindcss` natif. Les versions React 19.2.6 / Vite 8 / ESLint 10 inscrites dans le `package.json` original étaient également fictives — toutes remplacées par des versions stables réellement publiées.

### 11.5 Sources IDL dupliquées

**Symptôme** : Le module `corba-idl` contenait deux arborescences `src/java/` et `src/main/java/` avec les classes générées du compilateur IDL. Aucune n'était utilisée car le `pom.xml` configurait `sourceDirectory` sur `target/generated-sources/idl`.

**Solution** : Suppression des deux répertoires obsolètes. Réduction de la confusion sans impact sur le build.

### 11.6 Cartes prépayées refusées par Oracle Cloud

**Symptôme** : Oracle Cloud Always Free, premier choix de l'hébergeur (24 Go RAM ARM gratuit), refuse les cartes Wave Visa et autres cartes prépayées sénégalaises.

**Solution** : Pivot vers Render.com qui ne demande aucune carte. Adaptation de la stack pour rentrer dans 512 Mo (fusion supervisord + heap réduites).

### 11.7 Azure for Students refuse le domaine `.edu.sn`

**Symptôme** : Microsoft ne reconnaît pas automatiquement les emails `etu.ussein.edu.sn` comme académiques.

**Solution** : Choix de Render qui ne nécessite ni email académique ni preuve d'identité.

---

## 12. Conclusion et perspectives

### 12.1 Bilan

Le projet livre une suite PDF distribuée complète et fonctionnelle, déployée publiquement à https://corba-pdf.onrender.com. Les 14 services métier sont opérationnels, la pile technique respecte les standards de l'industrie (Docker, CI implicite via Git, HTTPS Let's Encrypt) et le code source est ouvert sur GitHub.

Au-delà de la simple validation des compétences en CORBA, le projet aborde la chaîne complète d'un système distribué moderne : modélisation par contrat (IDL), implémentation backend, passerelle de protocole, interface utilisateur, conteneurisation, déploiement cloud et résolution de problèmes en production.

### 12.2 Acquis pédagogiques

- Maîtrise du cycle CORBA (ORB, POA, IOR, Naming Service, stubs/skeletons).
- Compréhension du pattern Façade appliqué à un protocole binaire (CORBA → REST).
- Pratique du multi-module Maven et de la gestion centralisée des dépendances.
- Conception d'une interface utilisateur de qualité entreprise avec un design system cohérent.
- Industrialisation via Docker Compose et déploiement sur hébergeur cloud.
- Diagnostic en production : lecture de logs, ajustement de variables d'environnement, redéploiement à la volée.

### 12.3 Limites identifiées

- **RAM serrée en free tier** : 512 Mo limitent l'OCR sur les PDF scannés volumineux. Une montée en plan payant (1 Go) résoudrait le problème.
- **Cold start de 30 s** : inhérent au free tier Render. Un ping cron périodique (UptimeRobot) maintiendrait le service éveillé.
- **Pas de persistance utilisateur** : aucune base de données, aucune authentification. Les PDF sont traités à la volée puis oubliés.
- **CORBA en perte de vitesse industrielle** : choix pédagogique assumé.

### 12.4 Perspectives d'évolution

1. **Authentification JWT** pour permettre un usage multi-utilisateurs traçable.
2. **Persistance des résultats** dans un stockage objet (S3, MinIO) avec lien temporaire de téléchargement.
3. **File d'attente asynchrone** (Redis + worker) pour les traitements lourds (OCR de centaines de pages).
4. **Interface admin** : nombre d'appels, taille moyenne des PDF, distribution des opérations.
5. **Internationalisation** du frontend (français, anglais, wolof).
6. **Mode sombre** : tokens déjà préparés dans le design system, à brancher.
7. **Tests automatisés** : JUnit pour les handlers, Cypress pour les parcours UI, GitHub Actions pour la CI.
8. **Migration vers gRPC** comme alternative moderne à CORBA, en gardant la même architecture en couches.

---

## 13. Annexes

### 13.1 Structure du dépôt

```
corba-pdf-server/
├── pom.xml                          # parent Maven
├── docker-compose.yml               # orchestration locale (4 services)
├── Dockerfile.render                # image Render fusionnée
├── supervisord.conf                 # config supervisord prod
├── render.yaml                      # blueprint Render
├── build.sh / deploy.sh             # scripts de build/déploiement
├── nginx/nginx.conf                 # reverse proxy public
├── volumes/                         # bind mounts Docker locaux
│   ├── pdf-storage/
│   └── tessdata/
├── corba-idl/                       # contrats CORBA
│   ├── pom.xml
│   └── src/main/idl/
│       ├── Types.idl
│       └── PDFService.idl
├── corba-server/                    # implémentation serveur
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/sn/ussein/pdfserver/
│       │   ├── ServerMain.java
│       │   ├── impl/PDFServiceImpl.java
│       │   ├── handlers/            # 14 handlers
│       │   └── util/FileUtil.java
│       └── resources/jacorb.properties
├── api-gateway/                     # passerelle REST
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/sn/ussein/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── service/CorbaClientService.java
│       │   └── controller/PDFController.java
│       └── resources/application.yml
├── frontend/                        # SPA React
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   ├── index.html
│   ├── nginx-spa.conf
│   ├── Dockerfile
│   ├── public/favicon.svg
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       ├── api/pdfApi.js
│       ├── components/  (7 composants)
│       └── pages/       (15 pages dont NotFound)
├── README.md
└── RAPPORT.md                       # ce rapport
```

### 13.2 Commandes utiles

```bash
# Build local complet
./build.sh

# Lancer en local
docker compose up -d

# Voir les logs
docker compose logs -f api-gateway

# Arrêter
docker compose down

# Tester l'API
curl http://localhost/api/pdf/ping
curl -X POST -F "text=Hello" -F "title=Test" \
     http://localhost/api/pdf/create -o test.pdf

# Déployer (auto-deploy actif sur Render)
git push origin main

# Vérifier la prod
curl https://corba-pdf-api.onrender.com/api/pdf/ping
```

### 13.3 Webographie

- Specification CORBA : https://www.omg.org/spec/CORBA/
- JacORB : https://www.jacorb.org/
- Apache PDFBox : https://pdfbox.apache.org/
- Tess4J : http://tess4j.sourceforge.net/
- BouncyCastle : https://www.bouncycastle.org/
- Spring Boot 3 : https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/
- React 18 : https://react.dev/
- Vite : https://vitejs.dev/
- Tailwind CSS : https://tailwindcss.com/
- Docker Compose : https://docs.docker.com/compose/
- Render : https://render.com/docs

### 13.4 Statistiques du projet

| Métrique | Valeur |
|---|---|
| Modules Maven | 3 (corba-idl, corba-server, api-gateway) |
| Fichiers Java | 22 (hors stubs IDL générés) |
| Lignes Java | ~2 500 |
| Composants React | 7 réutilisables |
| Pages React | 15 |
| Lignes JSX/JS | ~1 800 |
| Endpoints REST | 16 |
| Opérations CORBA | 16 |
| Services Docker locaux | 4 |
| Build Maven (cold) | ~12 s |
| Build frontend Vite | ~3 s |
| Build Docker complet (cold) | ~5 min |
| Taille image runtime backend | ~280 Mo |
| Taille bundle JS (gzip) | ~83 Ko |

---

*Fin du rapport.*
