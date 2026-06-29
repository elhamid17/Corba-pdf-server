# PDF Suite

[![CI](https://github.com/elhamid17/Corba-pdf-server/actions/workflows/ci.yml/badge.svg)](https://github.com/elhamid17/Corba-pdf-server/actions/workflows/ci.yml)

Suite de traitement PDF **open-core, self-host-first, local-first** : 40+ outils (organisation,
conversion, sécurité, analyse, génération) exposés via une API REST documentée et un frontend React.

Monolithe modulaire Spring Boot (Java 21). Le traitement PDF tourne **in-process** — il n'y a plus
de couche CORBA (cf. [historique d'architecture](docs/ARCHITECTURE.md)).

## Stack technique

| Couche | Technologie |
|---|---|
| Frontend | React 18 + Vite + TailwindCSS |
| API | Spring Boot 3 (Java 21) — REST/JSON, versionnée `/api/v1`, documentée OpenAPI/Swagger |
| Moteur PDF | module `pdf-engine` in-process — Apache PDFBox + Tess4J (OCR) + BouncyCastle + POI + ZXing |
| Persistance | MongoDB (comptes, jobs, GridFS) |
| Sécurité | Spring Security + JWT, rate-limiting, quotas, en-têtes durcis (CSP/HSTS en prod) |
| Infrastructure | Docker Compose + Nginx ; CI/CD GitHub Actions + image GHCR |

## Architecture

```
[React/Vite] ──HTTP/JSON──> [Nginx] ──> [API Gateway Spring Boot, JVM unique]
                                              ├── module web (contrôleurs /api/v1 par domaine, auth, quotas)
                                              ├── module pdf-engine (40+ handlers, in-process)
                                              └── persistance (MongoDB : users, jobs)
```

## Services PDF

Organisation (fusion, découpage, extraction/suppression/réordonnancement/rotation de pages, numérotation,
redimensionnement, recadrage), conversion (compression, PDF↔images/Word/Excel/PowerPoint/Markdown/HTML/ODT,
PDF/A), sécurité (mot de passe, déverrouillage, signature numérique & manuscrite, caviardage, anonymisation),
analyse (extraction de texte, **OCR Tesseract**, métadonnées, vérification de signature, comparaison,
statistiques), génération (création, filigrane, page de garde, tampon, QR/code-barres, CV).

## Lancement rapide (dev)

```bash
docker compose up --build
```

- Frontend : http://localhost
- API : http://localhost/api/v1
- Swagger UI : http://localhost/swagger-ui.html *(dev uniquement ; désactivé en prod)*

## Déploiement (prod)

Profil `prod` activé via `SPRING_PROFILES_ACTIVE=prod`. Le démarrage **échoue volontairement** si
`JWT_SECRET` ou `ADMIN_PASSWORD` sont absents ou laissés aux valeurs par défaut (cf. `ProdSecretsValidator`).
Image conteneur publiée sur GHCR par la CI (push sur `main`).

## Build & tests

```bash
mvn clean verify              # backend : pdf-engine + api-gateway (tests unitaires + intégration Testcontainers)
cd frontend && npm ci && npm test && npm run build
```

## Documentation projet

- [Roadmap](docs/ROADMAP.md) — versions V1→V6 et leur statut
- [Architecture](docs/ARCHITECTURE.md)
- [Décisions (ADR)](docs/DECISIONS.md)

## Auteur

Projet initié en L2 AgroTIC (USSEIN), en évolution vers un produit SaaS.
