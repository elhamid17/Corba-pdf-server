# Prompt de passation — V3 / Étape 3.1 — Traitement asynchrone des opérations lourdes

> À copier-coller dans une conversation dédiée. Prérequis : V2 livrée et mergée dans `main` (CI verte).
> ⚠️ Déléguer SANS isolation worktree. Créer une branche `v3/production` à partir de `main`.

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, Java 21, moteur in-process `PdfEngine`.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL
- Tous les traitements PDF sont **synchrones** : le contrôleur appelle `pdfEngine.xxx()` puis
  `PdfResponseSupport.buildAndRecord(...)` enregistre un `Job` (statut `SUCCESS`/`FAILED`) et renvoie les octets inline.
- `JobStatus` = `PENDING, SUCCESS, FAILED` (PENDING existe mais inutilisé).
- `JobStorageService` : `checkQuota`, `recordSuccess`, `loadResult` (GridFS), `delete`, `canAccess`.
- `JobController` : `GET /api/v1/jobs` (liste), `GET /api/v1/jobs/{id}/download`, `DELETE /api/v1/jobs/{id}`.
- Problème : OCR et gros fichiers bloquent le thread requête (timeouts, mauvaise UX, fragilité).

## §3. OBJECTIF STRICT
Permettre l'exécution **asynchrone** des opérations lourdes, sans casser les opérations légères ni le frontend.

DANS LE PÉRIMÈTRE :
1. Introduire un **chemin asynchrone** : un endpoint de soumission renvoie **202 Accepted + `jobId`**
   immédiatement ; le traitement s'exécute en arrière-plan ; le client suit via `GET /api/v1/jobs/{id}`
   (statut) puis récupère le résultat via le `GET /jobs/{id}/download` existant.
2. Ajouter le statut **`RUNNING`** à `JobStatus` et un endpoint **`GET /api/v1/jobs/{id}`** (statut + métadonnées,
   sans le binaire).
3. **Exécuteur en arrière-plan** : `@Async`/`ThreadPoolTaskExecutor` (ou worker dédié), avec persistance de l'état
   du job en Mongo (PENDING→RUNNING→SUCCESS/FAILED) et résultat stocké en GridFS (réutiliser `loadResult`).
   Profiter de Java 21 (virtual threads envisageables).
4. **Cibler les opérations lourdes** : au minimum **OCR** ; idéalement un critère (taille de fichier au-delà d'un
   seuil → bascule async). Garder les opérations légères **synchrones** (UX instantanée préservée).
5. Quotas/sécurité : `checkQuota` et la propriété du job (`canAccess`) s'appliquent au chemin async aussi.

HORS PÉRIMÈTRE :
- La progression fine en temps réel (0→100 %) = étape 3.2 (SSE). Ici, un statut discret suffit.
- Observabilité (3.3), stockage objet S3/MinIO (3.4).
- Refonte du frontend : prévoir le contrat (202 + poll) ; l'intégration UI complète peut être minimale/différée
  (à décider — au minimum, ne rien casser des appels synchrones existants).

## §4. INVARIANTS
1. Les endpoints synchrones existants continuent de fonctionner à l'identique (frontend non cassé).
2. `mvn clean package` vert (+ nouveaux tests du chemin async, y compris intégration Mongo).
3. Sécurité/quotas appliqués au chemin async (pas de contournement).
4. **Validation locale (docker compose) avant tout commit/push** ; l'utilisateur valide d'abord.

## §5. DEFINITION OF DONE
- Soumission async → 202 + `jobId` ; `GET /jobs/{id}` reflète `PENDING→RUNNING→SUCCESS/FAILED` ;
  téléchargement du résultat via l'endpoint existant.
- OCR (au moins) passe par le chemin async ; opérations légères inchangées.
- Tests couvrant le cycle de vie async (soumission, polling, succès, échec, accès non autorisé).
- `mvn clean package` vert. ADR du choix async (exécuteur, seuil, contrat) dans `docs/DECISIONS.md`.

## §6. GARDE-FOUS
- ❌ NE PAS rendre TOUTES les opérations async (les légères doivent rester instantanées).
- ❌ NE PAS perdre la sécurité/quota sur le chemin async.
- ❌ NE PAS bloquer indéfiniment : prévoir échec/timeout propre côté worker (statut FAILED + message).
- ❌ NE PAS pousser sans validation locale de l'utilisateur.

## §7. PROTOCOLE DE RETOUR
1. Contrat du chemin async (endpoints, codes, forme du statut) + ADR.
2. Mécanisme d'exécution (executor, threads/virtual threads, persistance d'état).
3. Opérations basculées en async + critère retenu (OCR / seuil de taille).
4. Tests ajoutés + résultat `mvn clean package`.
5. Impact frontend (ce qui est fait / ce qui reste) et risques pour 3.2 (SSE).
6. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 3.2 : …
