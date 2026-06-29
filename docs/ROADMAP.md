# Roadmap — PDF Suite

> Pilotée par le Tech Lead. Chaque version se découpe en étapes ; chaque étape majeure =
> une conversation dédiée pilotée par un prompt de passation (`docs/handoffs/`).
> Dernière mise à jour : 2026-06-02.

## North Star
SaaS PDF **open-core, self-host-first, local-first**, dont le fer de lance est l'**automatisation
par workflows chaînés**. Évolution chirurgicale (jamais de rewrite). Monolithe modulaire Spring Boot, Java 21.

## Échelle des versions

| Version | Thème | Objectif macro | État |
|---|---|---|---|
| **V1** | 🔪 Excision de CORBA (Walking Skeleton) | Monolithe Spring Boot unique, handlers in-process, mêmes features, tests verts, déploiement simplifié. Zéro régression. | ✅ **Livrée** (mergée dans `main`, validée runtime) |
| **V2** | 🧱 Socle qualité & domaine | Casser le contrôleur de 1190 lignes, frontières de modules, OpenAPI, versioning d'API, durcissement sécurité/auth. | ✅ **Livrée** (mergée dans `main`, PR #1, CI 5/5 verte) |
| **V3** | ⚙️ Production-ready | Queue de jobs (OCR/gros fichiers), progression SSE, stockage objet, observabilité (OTel, métriques, logs structurés). | 🟡 Prochaine |
| **V4** | 🗡️ Workflows chaînés | Moteur de pipelines réutilisables (ex. OCR→anonymise→signe→tampon). Le différenciateur produit. | ⚪ Prévu |
| **V5** | 🔒 Local-first / confidentialité | Traitement client WASM pour ops sensibles, zéro-rétention garantie, auto-hébergement 1 commande, RGPD. | ⚪ Prévu |
| **V6** | 💎 IA + commercialisation | IA documentaire (BYO-key/modèle local), multi-tenant, facturation, SLA, SRE. Verticale = packaging GTM. | ⚪ Prévu |

---

## Découpage V1 — Excision de CORBA

> But : prouver que tout fonctionne **sans CORBA**, sans rien perdre. Le geste fondateur du projet.

| Étape | Intitulé | Livrable clé |
|---|---|---|
| **1.1** | Module `pdf-engine` in-process | ✅ **TERMINÉ** — interface `PdfEngine` (54 méthodes) + `PdfEngineImpl` + 41 handlers + POJO `.model`, sans CORBA. `mvn -pl pdf-engine test` = **62/62 vert, BUILD SUCCESS**. Branche `worktree-agent-a9ab7f0a872ae112b`. |
| **1.2** | Branchement gateway | ✅ **TERMINÉ** — `PDFController` branché sur `PdfEngine` in-process ; `CorbaClientService`/JacORB/`corba-idl`/`corba-server` supprimés ; gateway en Java 21. `mvn clean package` = **pdf-engine 62 + api-gateway 74 tests, BUILD SUCCESS**. Correctifs Tech Lead : conflit SLF4J (slf4j-simple exclu), stubs Mockito lenient, 400 params manquants. Commits `7971e71`+`f43a51f`. |
| **1.3** | Nettoyage infra | ✅ **TERMINÉ (code/infra)** — `corba-server` + supervisord supprimés ; docker-compose = mongo+gateway+frontend+nginx ; Dockerfiles mono-process Java 21 + Tesseract/polices dans l'image gateway ; `render.yaml` mono-service. Grep CORBA infra vide. Commit `c18eca7`. ⏳ Reste la **validation runtime** par l'utilisateur (`docker compose up` + test OCR/conversion). |

**Definition of Done V1 :** `docker compose up` lance frontend + 1 backend + mongo ; les 40+ outils
fonctionnent comme avant ; toute la suite de tests est verte ; plus aucune trace de CORBA/JacORB/IOR/supervisord.

### Statut V1 : ✅ LIVRÉE — mergée dans `main` (`7d9bba0`, poussée sur origin)
- Code mono-service (1.1 + 1.2) + infra alignée (1.3) : zéro CORBA/supervisord.
- Validée runtime : `docker compose up` → 4 conteneurs healthy, ping/merge/create/**OCR** OK.

---

---

## Découpage V2 — Socle qualité & domaine

> But : rendre le backend maintenable et présentable avant d'ajouter des features (V4+).
> Le contrat REST reste stable sur toute la V2 (le frontend ne bouge pas).

| Étape | Intitulé | Livrable clé | État |
|---|---|---|---|
| **2.1** | Découpage du `PDFController` | ✅ **TERMINÉ** — 1190 LOC → 6 contrôleurs par domaine (Organisation/Conversion/Security/Analysis/Generation/Ping) + `PdfResponseSupport` factorisant validation/réponses/jobs. Contrat REST identique (44 endpoints PDF préservés). `mvn clean package` = 62+74 tests, BUILD SUCCESS. Commit `578b165`. |
| **2.2** | OpenAPI + versioning d'API | ✅ **TERMINÉ** — springdoc/Swagger UI (9 `@Tag`), bascule `/api/v1` (stratégie A, ADR-0007/0008) via source unique `ApiPaths` (back) + `routes.js` (front). `mvn` 62+74 verts, `npm run build` vert, zéro chemin ancien résiduel. Commit `c0a557d`. |
| **2.3** | Durcissement sécurité | ✅ **TERMINÉ** — `ProdSecretsValidator` (fail-fast JWT/admin en prod), CSP + HSTS conditionnel + en-têtes, Swagger désactivé en prod, revue rate-limit/CORS/upload. ADR-0009/0010. Vérifié runtime (curl dev+prod). `mvn` 62+74 vert. Commit `bd61845`. |
| **2.4** | CI/CD | ✅ **TERMINÉ** — `ci.yml` Java 21, reactor complet (`mvn -B clean verify`), 5 jobs (backend/frontend/e2e/docker/security) avec `needs`+`concurrency`. Dependabot (maven+npm+actions) + scan Trivy. Image GHCR build à chaque run, push sur `main` via `GITHUB_TOKEN`. Fiabilisation : Vitest exclut `e2e/**`, mock e2e corrigé (`/api/v1`). ADR-0011. Tout vert local (62+74, vitest 17, e2e 2, build). |

### Statut V2 : ✅ LIVRÉE — mergée dans `main` (PR #1, merge `4554409`)
- Backend maintenable : `PDFController` éclaté en 6 contrôleurs de domaine (2.1).
- API présentable et versionnée : OpenAPI/Swagger + `/api/v1` source unique (2.2).
- Durcissement : profil prod, fail-fast secrets, CSP/HSTS, Swagger off en prod (2.3).
- Pipeline fiable et moderne : CI/CD Java 21, scan dépendances, image GHCR (2.4).
- **CI GitHub Actions 5/5 verte** sur la PR (backend/frontend/e2e/docker/security).

---

## Découpage V3 — Production-ready

> But : passer d'« ça marche » à « ça tient en production ». Opérations lourdes asynchrones,
> retour de progression, observabilité. Contrat REST étendu (nouveaux endpoints jobs/SSE), pas cassé.

| Étape | Intitulé | Livrable clé | État |
|---|---|---|---|
| **3.1** | Traitement asynchrone des opérations lourdes | File de jobs (OCR, gros fichiers, conversions) : `submit → 202 + jobId → poll`. Exécution en arrière-plan, état persistant. S'appuie sur `Job`/`JobStorageService`/`JobController` existants. | 🟡 Prêt — `handoffs/V3-etape-3.1-jobs-async.md` |
| **3.2** | Progression temps réel (SSE) | Endpoint SSE pour suivre l'avancement d'un job (0→100 %, statut, résultat). | ⚪ Prévu |
| **3.3** | Observabilité | OpenTelemetry (traces), Micrometer/Prometheus (métriques), logs JSON structurés, `/actuator`. | ⚪ Prévu |
| **3.4** | Abstraction stockage objet | Abstraire GridFS pour permettre S3/MinIO en self-host (rétention, cycle de vie). | ⚪ Prévu |

---

## Suivi des passations
Voir `docs/handoffs/` — un fichier par étape, avec le prompt prêt à injecter et la rétrospective en fin d'étape.
