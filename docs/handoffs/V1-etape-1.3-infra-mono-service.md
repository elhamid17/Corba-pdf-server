# Prompt de passation — V1 / Étape 1.3 — Infra mono-conteneur/2 JVM → mono-service

> À copier-coller dans une conversation dédiée. Prérequis : étapes 1.1 + 1.2 terminées et validées
> (`mvn clean package` global vert : pdf-engine 62 tests, api-gateway 74 tests). Branche : `v1/decorba`.
> ⚠️ Déléguer SANS isolation worktree (ou merger d'abord dans `main`) — l'isolation worktree part de `main`
> et ne verrait pas le travail 1.1/1.2. Cf. mémoire « Délégation worktree ».

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, **Java 21**, SANS CORBA.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL (sortie de 1.2)
- Le backend est UN seul déployable : `api-gateway` (Spring Boot) embarque `pdf-engine` (41 handlers in-process).
- Plus aucun code CORBA. Modules `corba-idl`/`corba-server` supprimés. Java 21.
- **MAIS l'infra n'a pas suivi** — tout référence encore CORBA/supervisord :
  - `docker-compose.yml` : service `corba-server` + dépendance + env `CORBA_HOST/PORT` + volume IOR `pdf-storage`.
  - `Dockerfile.render` : build de `corba-server` + 2 JVM via **supervisord** dans un seul conteneur.
  - `supervisord.conf` : orchestre les 2 process Java (corba-server + gateway).
  - `render.yaml` : env `CORBA_HOST/PORT`, double `JAVA_OPTS_CORBA`/`JAVA_OPTS_GATEWAY`.
  - `api-gateway/Dockerfile` : Java **17**, passe `--corba.host/--corba.port`.
  - `build.sh` / `deploy.sh` / `nginx/nginx.conf` : références CORBA résiduelles.

## §3. OBJECTIF STRICT
Aligner l'infra sur l'archi mono-service : un seul process Java, plus de CORBA, plus de supervisord.

DANS LE PÉRIMÈTRE :
1. `api-gateway/Dockerfile` : base **Java 21** ; retirer `CORBA_HOST/PORT` et les flags `--corba.*` ;
   **installer Tesseract + données de langue (fra/eng) + polices (fontconfig/dejavu)** — l'OCR et le rendu
   PDF tournent désormais dans CETTE JVM (avant : seulement dans l'image corba-server). Idéalement build
   multi-stage (compile le reactor, package le fat jar `api-gateway`).
2. `docker-compose.yml` : supprimer le service `corba-server`, sa dépendance, les env `CORBA_*`, le partage
   du volume IOR. Garder mongo + api-gateway + frontend + nginx. Le volume `tessdata` doit être monté sur la gateway.
3. Supprimer `supervisord.conf` et `Dockerfile.render` (mono-conteneur 2 JVM) — ou réduire `Dockerfile.render`
   à un seul process `java -jar api-gateway.jar` (selon la cible Render mono-service).
4. `render.yaml` : un seul web service, un seul `JAVA_OPTS`, sans env CORBA.
5. Nettoyer `build.sh`, `deploy.sh`, `nginx/nginx.conf` des références CORBA.

HORS PÉRIMÈTRE :
- Le code applicatif (gateway/pdf-engine) — il est déjà bon.
- Le frontend.
- Toute optimisation au-delà du strict alignement mono-service (mémoire, multi-stage avancé = plus tard).

## §4. INVARIANTS À NE JAMAIS CASSER
1. `docker compose up` démarre : mongo + api-gateway + frontend + nginx (PLUS de corba-server).
2. Les 40+ outils fonctionnent end-to-end via l'UI, **OCR inclus** (Tesseract présent dans l'image gateway).
3. `mvn clean package` reste vert.
4. **Validation locale (docker compose) AVANT tout commit/push** : l'utilisateur valide d'abord.

## §5. RISQUES / POINTS D'ATTENTION
1. **Tesseract + tessdata + polices** : c'est LE piège. Sans eux dans l'image gateway, OCR et certaines
   conversions/rendus échoueront au runtime alors que les tests unitaires passent. Vérifier `TESSDATA_PREFIX`/
   chemin tessdata côté gateway.
2. **Mémoire** : un seul JVM remplace deux — réajuster `JAVA_OPTS` (le free tier Render = 512 Mo).
3. **Volume `pdf-storage`** : ne servait qu'à partager l'IOR ; les jobs sont en Mongo/GridFS. Vérifier qu'aucun
   code ne lit/écrit encore ce volume avant de le retirer.
4. **Healthcheck** : `/api/pdf/ping` doit toujours répondre (il appelle `pdfEngine.ping()` maintenant).

## §6. DEFINITION OF DONE
- `docker compose up --build` : stack fonctionnelle, aucun conteneur corba-server, aucun supervisord.
- `grep -rIn "corba\|CORBA\|supervisor\|2809\|\.ior\|IOR" docker-compose.yml Dockerfile.render render.yaml build.sh deploy.sh api-gateway/Dockerfile nginx/` → vide (hors commentaires d'historique éventuels).
- Test manuel : merge + OCR + une conversion fonctionnent via l'UI.

## §7. GARDE-FOUS
- ❌ NE PAS modifier le code applicatif.
- ❌ NE PAS retirer Tesseract/polices « parce qu'on ne les voit pas dans les tests ».
- ❌ NE PAS pousser sans validation locale `docker compose` de l'utilisateur.

## §8. PROTOCOLE DE RETOUR
1. Liste des fichiers infra modifiés/supprimés.
2. Résultat `docker compose up --build` (services up + healthchecks).
3. Confirmation du grep CORBA vide sur l'infra.
4. Test manuel OCR + conversion (capture/log).
5. Note de clôture **V1** : ce qui reste éventuellement avant d'attaquer la V2 (socle qualité).

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Clôture V1 / amorçage V2 : …
