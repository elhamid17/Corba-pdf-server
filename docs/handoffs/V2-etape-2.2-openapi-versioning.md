# Prompt de passation — V2 / Étape 2.2 — OpenAPI + versioning d'API

> À copier-coller dans une conversation dédiée. Prérequis : étape 2.1 terminée et validée
> (6 contrôleurs par domaine + `PdfResponseSupport`, `mvn clean package` vert).
> ⚠️ Déléguer SANS isolation worktree. Travailler sur la branche `v2/quality` (suite de 2.1).

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, Java 21, moteur in-process `PdfEngine`.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL (sortie de 2.1)
- Contrôleurs PDF : `Organisation/Conversion/Security/Analysis/Generation/Ping`, tous sous `@RequestMapping("/api/pdf")`.
- Contrôleurs hors PDF : `AuthController` (`/api/auth/...`), `AdminController` (`/api/admin/...` + `/api/admin/stats`),
  `JobController` (`/api/jobs...`).
- **Frontend** : `frontend/src/api/client.js` expose `API_BASE` (racine) ; les chemins absolus
  (`/api/pdf/...`, `/api/auth/...`, `/api/jobs`, `/api/admin/...`) sont **codés en dur** dans
  `frontend/src/api/{pdfApi,authApi,adminApi,jobsApi}.js`.
- **nginx** : `location /api/ { proxy_pass http://api-gateway:8080/api/; }` — proxifie tout `/api/` (donc `/api/v1/` aussi).

## §3. OBJECTIF STRICT
Documenter l'API (OpenAPI/Swagger) et la versionner proprement en `/api/v1`.

DANS LE PÉRIMÈTRE :
1. **OpenAPI/Swagger (ajout pur, aucune rupture)** : ajouter `springdoc-openapi-starter-webmvc-ui` à `api-gateway`.
   Exposer Swagger UI + `/v3/api-docs`. Annoter chaque contrôleur d'un `@Tag` par domaine ; titre/description/version
   de l'API via `@OpenAPIDefinition` ou config. S'assurer que les routes doc/swagger sont accessibles (whitelist sécurité).
2. **Versioning `/api/v1`** : préfixer toutes les routes applicatives par `/v1`
   (→ `/api/v1/pdf/...`, `/api/v1/auth/...`, `/api/v1/admin/...`, `/api/v1/jobs...`). Implémenter via une **constante
   partagée** (ex. `ApiPaths.V1 = "/api/v1"`) ou un préfixe centralisé — PAS 9 chaînes dupliquées en dur.
3. **Frontend** : répercuter le préfixe en UN seul endroit autant que possible (centraliser les bases dans
   `client.js` / un module de routes), de sorte que les appels passent par `/api/v1/...`.
4. Adapter les tests (chemins `/api/v1/...`) ; tout reste vert. Vérifier nginx (le proxy `/api/` couvre déjà `/api/v1/`).

HORS PÉRIMÈTRE :
- Changer la logique métier ou les formats de réponse.
- Sécurité au-delà de la whitelist des routes Swagger (durcissement = étape 2.3).
- CI/CD (2.4).

## §4. DÉCISION D'ARCHITECTURE À ACTER (le point sensible)
Le versioning **change les URLs**, donc le frontend DOIT suivre. Deux stratégies acceptables — choisir et documenter :
- **(A) Bascule franche** `/api/v1` partout + MAJ frontend (recommandé pour la propreté). Le frontend et le backend
  changent en lockstep ; nginx inchangé.
- **(B) `/api/v1` canonique + ancien `/api` maintenu en alias déprécié** (zéro rupture immédiate, migration douce).
Reco Tech Lead : **(A)** si on centralise bien les chemins ; sinon (B) comme filet. Documenter le choix dans un ADR.

## §5. INVARIANTS
1. Aucun changement de comportement (mêmes réponses) — seules les URLs gagnent `/v1`.
2. `mvn clean package` vert ; frontend qui build (`npm run build`) et fonctionne.
3. Swagger UI accessible sans casser la sécurité (routes doc whitelistées).
4. **Validation locale (docker compose + UI) avant tout commit/push** ; l'utilisateur valide d'abord.

## §6. DEFINITION OF DONE
- Swagger UI opérationnelle, listant tous les endpoints groupés par domaine.
- Toutes les routes applicatives sous `/api/v1`, via une source unique (constante/config).
- Frontend appelle `/api/v1/...` et fonctionne end-to-end (merge/ocr/login/jobs).
- `mvn clean package` vert ; build frontend vert.
- ADR du choix de versioning ajouté à `docs/DECISIONS.md`.

## §7. GARDE-FOUS
- ❌ NE PAS disperser le préfixe `/api/v1` en chaînes dupliquées (backend ET frontend → source unique).
- ❌ NE PAS modifier les corps/codes de réponse.
- ❌ NE PAS pousser sans validation locale de l'utilisateur.

## §8. PROTOCOLE DE RETOUR
1. Stratégie de versioning retenue (A ou B) + ADR rédigé.
2. URL Swagger UI + capture de la liste des endpoints documentés.
3. Liste des fichiers backend/frontend touchés + où vit la source unique du préfixe.
4. Résultat `mvn clean package` + `npm run build`.
5. Risques pour l'étape 2.3 (durcissement sécurité).
6. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 2.3 : …
