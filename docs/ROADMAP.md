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
| **V1** | 🔪 Excision de CORBA (Walking Skeleton) | Monolithe Spring Boot unique, handlers in-process, mêmes features, tests verts, déploiement simplifié. Zéro régression. | 🟡 À démarrer |
| **V2** | 🧱 Socle qualité & domaine | Casser le contrôleur de 1190 lignes, frontières de modules, OpenAPI, versioning d'API, durcissement sécurité/auth. | ⚪ Prévu |
| **V3** | ⚙️ Production-ready | Queue de jobs (OCR/gros fichiers), progression SSE, stockage objet, observabilité (OTel, métriques, logs structurés). | ⚪ Prévu |
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

### Statut V1 : 🟢 code & infra bouclés — en attente de validation runtime utilisateur
- Code mono-service (1.1 + 1.2) : `mvn clean package` vert (pdf-engine 62 + api-gateway 74 tests).
- Infra alignée (1.3) : zéro CORBA/supervisord, image gateway autonome (OCR inclus).
- **Avant clôture définitive / merge `v1/decorba` → `main`** : l'utilisateur valide `docker compose up --build` (4 conteneurs, healthcheck vert) + test manuel OCR + une conversion.

---

---

## Découpage V2 — Socle qualité & domaine

> But : rendre le backend maintenable et présentable avant d'ajouter des features (V4+).
> Le contrat REST reste stable sur toute la V2 (le frontend ne bouge pas).

| Étape | Intitulé | Livrable clé | État |
|---|---|---|---|
| **2.1** | Découpage du `PDFController` | Casser les 1190 LOC en contrôleurs par domaine (organisation/transformation/conversion/sécurité/analyse/génération) + base commune éliminant la duplication (try/catch/validation/record). Contrat REST identique, tests verts. | 🟡 Prêt — `handoffs/V2-etape-2.1-decoupage-controller.md` |
| **2.2** | OpenAPI + versioning d'API | springdoc/Swagger UI, préfixe `/api/v1`, schémas documentés. | ⚪ Prévu |
| **2.3** | Durcissement sécurité | Headers de sécurité, validation centralisée, revue JWT/rate-limit/quotas, gestion des secrets. | ⚪ Prévu |
| **2.4** | CI/CD | GitHub Actions (build + tests + lint) à partir du `.github/workflows/ci.yml` existant ; image Docker publiée. | ⚪ Prévu |

---

## Suivi des passations
Voir `docs/handoffs/` — un fichier par étape, avec le prompt prêt à injecter et la rétrospective en fin d'étape.
