# Tests — CORBA PDF Server

## Backend (Maven)

```bash
mvn test
```

**Prérequis intégration MongoDB :** Docker doit être démarré (Testcontainers lance `mongo:7`).

Sur **Docker 29+**, le fichier `api-gateway/src/test/resources/docker-java.properties` force `api.version=1.44` (compatibilité docker-java / Testcontainers 1.x).

| Module | Tests | Couverture |
|--------|-------|------------|
| `corba-server` | Handlers PDF (~28 handlers) | PDFBox réel, PDFs/PNG synthétiques |
| `api-gateway` | JWT, auth, quotas, rate limit, PDFController par catégorie | Mockito + MockMvc standalone |
| `api-gateway` | **Intégration MongoDB** (`integration/*IntegrationTest`) | Testcontainers + GridFS réel |

### Tests MongoDB (Testcontainers)

Fichiers dans `api-gateway/src/test/java/sn/ussein/gateway/integration/` :

| Classe | Vérifie |
|--------|---------|
| `JobStorageServiceIntegrationTest` | Persistance GridFS, ACL guest/user, quotas cumulatifs, compteur stockage `$inc` |
| `JobRepositoryIntegrationTest` | Requêtes Mongo `findByUserId`, `findByGuestId`, `countByCreatedAtAfter` |
| `JobControllerIntegrationTest` | HTTP `GET/DELETE /api/jobs` — liste, download, ACL guest/user (MockMvc) |
| `AuthControllerIntegrationTest` | HTTP register, login, `/me` |
| `AdminControllerIntegrationTest` | HTTP `/api/admin/*` — stats, users, jobs, ACL ADMIN |

### PDFController (MockMvc standalone)

| Classe | Catégorie |
|--------|-----------|
| `PDFControllerTest` | ping, merge |
| `PDFControllerOrganisationTest` | split, extract-pages, delete-pages, reorder, reverse |
| `PDFControllerTransformTest` | rotate, compress, watermark, resize, crop |
| `PDFControllerReadOnlyTest` | extract-text, metadata, page-count, stats, create |

Handlers PDF testés : merge, split, rotate, compress, password, extract, delete-pages, text-extract, watermark, create, reverse, reorder, metadata, page-numbers, resize, crop, anonymize, stats, markdown/html, unlock, redact, stamp, compare, images-to-pdf, convert-to-images, pdf-to-markdown, barcode/QR, cover, signature-image.

Non couverts (dépendances lourdes : Office, Tesseract, certificats X.509) : OCR, sign, verify-signature, word/excel/odt/pptx conversions, PdfA, CV builder.

### CI GitHub Actions

Workflow `.github/workflows/ci.yml` : `mvn test` + `npm test` + Playwright E2E sur chaque push/PR vers `main`.

Lancer uniquement les tests d'intégration :

```bash
mvn -pl api-gateway test -Dtest='*IntegrationTest'
```

## Frontend (Vitest)

```bash
cd frontend && npm test
```

| Fichier | Sujet |
|---------|--------|
| `services.test.js` | Catalogue des 35 outils |
| `useToolHistory.test.js` | Favoris / récents (localStorage) |
| `workflowSuggestions.test.js` | Routes v2 du workflow chaining |
| `DropZone.test.jsx` | Drag & drop, sélection fichier |
| `LoginPage.test.jsx` | Formulaire connexion, validation, navigation |

## E2E (Playwright)

```bash
cd frontend && npm ci && npx playwright install chromium
npm run test:e2e
```

Scénario `e2e/merge.spec.js` : upload 2 PDF → fusion → téléchargement (API mockée pour fiabilité CI).

## Prochaines étapes suggérées

- E2E full-stack avec docker-compose (sans mock API)
- Handlers Office / OCR / signature numérique (fixtures dédiées)
- Couverture Jacoco / seuils CI
