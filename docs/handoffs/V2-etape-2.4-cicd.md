# Prompt de passation — V2 / Étape 2.4 — CI/CD (clôture V2)

> À copier-coller dans une conversation dédiée. Prérequis : étapes 2.1→2.3 terminées et validées
> (contrôleurs par domaine, `/api/v1`+Swagger, durcissement sécurité, builds verts).
> ⚠️ Déléguer SANS isolation worktree. Branche `v2/quality`.

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, Java 21.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL
- `.github/workflows/ci.yml` existe mais **obsolète** : **JDK 17** (le projet est en **21**), lance `mvn test`,
  `npm test`, Playwright e2e. Pas de scan de vulnérabilités, pas de build d'image.
- Build local vert : `mvn clean package` (pdf-engine 62 + api-gateway 74), `npm run build`.
- Profil prod : secrets fail-fast (`ProdSecretsValidator`), Swagger désactivé, `SPRING_PROFILES_ACTIVE=prod`.
- Déploiement : `api-gateway/Dockerfile` (multi-stage Java 21 + Tesseract) ; `Dockerfile.render` mono-service.

## §3. OBJECTIF STRICT
Un pipeline CI/CD fiable et moderne qui garde le projet vert et publie un artefact déployable.

DANS LE PÉRIMÈTRE :
1. **Moderniser `ci.yml`** : JDK **21**, build du reactor complet (`mvn -B clean verify`), cache Maven/npm.
   Vérifier que les tests Testcontainers tournent (Docker dispo sur `ubuntu-latest`).
2. **Découpage en jobs lisibles** : backend (mvn), frontend (build + tests), e2e (Playwright) — avec
   `needs`/parallélisme raisonnable et `fail-fast`.
3. **Scan de vulnérabilités des dépendances** : activer Dependabot (`.github/dependabot.yml`, écosystèmes maven +
   npm + github-actions) et/ou un scan dans le pipeline (ex. Trivy/OWASP). Choix documenté.
4. **Build & publication d'image Docker** (sur push `main` uniquement) : construire l'image gateway et la pousser
   (ex. GHCR `ghcr.io/<owner>/<repo>`), taggée par sha + `latest`. Si les secrets registry ne sont pas garantis,
   au minimum **builder** l'image en CI (sans push) et documenter l'activation du push.
5. **Badge CI** dans le `README` (optionnel mais propre).

HORS PÉRIMÈTRE :
- Déploiement continu réel vers Render/prod (peut être préparé mais pas déclenché sans accord).
- Refactor applicatif.

## §4. INVARIANTS
1. Le pipeline reflète la réalité : s'il est vert, `mvn clean package` + `npm run build` le sont localement aussi.
2. Pas de secret en clair dans les workflows (utiliser `secrets.*`).
3. Les e2e ne doivent pas rendre le pipeline ingérablement flaky : si instables, les isoler en job non bloquant
   ou les fiabiliser, en le documentant.
4. **Validation : ouvrir une PR `v2/quality` → `main` pour voir le pipeline s'exécuter** ; l'utilisateur valide
   le résultat CI avant merge. Pas de push forcé.

## §5. DEFINITION OF DONE
- `ci.yml` en Java 21, reactor complet, jobs clairs, vert sur la branche.
- Dependabot (et/ou scan) configuré.
- Image Docker construite en CI (push conditionnel documenté).
- Note de **clôture V2** : récap des 4 étapes + état du socle qualité.

## §6. GARDE-FOUS
- ❌ NE PAS laisser JDK 17 dans le workflow (incohérent avec le code Java 21).
- ❌ NE PAS committer de credentials registry.
- ❌ NE PAS rendre le pipeline « toujours vert » en désactivant les tests — fiabiliser, pas masquer.

## §7. PROTOCOLE DE RETOUR
1. Contenu final de `ci.yml` (jobs, déclencheurs) + `dependabot.yml`.
2. Stratégie image Docker (build seul vs push GHCR) + comment l'activer.
3. Résultat attendu du pipeline (ce qui tourne, durée approximative, points flaky éventuels).
4. Note de clôture V2 + amorçage **V3 (production-ready : queue de jobs, SSE, observabilité)**.
5. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Clôture V2 / amorçage V3 : …
