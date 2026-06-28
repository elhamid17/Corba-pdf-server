# Prompt de passation — V2 / Étape 2.3 — Durcissement sécurité

> À copier-coller dans une conversation dédiée. Prérequis : étapes 2.1 + 2.2 terminées et validées
> (contrôleurs par domaine, `/api/v1`, Swagger, builds verts).
> ⚠️ Déléguer SANS isolation worktree. Branche `v2/quality`.

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, Java 21.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL (sortie de 2.2)
- Sécurité : `SecurityConfig` (Spring Security), `JwtService`/`JwtAuthenticationFilter`, `RateLimitFilter`,
  `GuestCookieFilter`, quotas (`JobStorageService`/`QuotaProperties`), CORS (`CorsConfig`).
- Config : `JWT_SECRET` a un **défaut** `change-me-in-production-...` (docker-compose) ; bootstrap admin via
  `AdminBootstrapper`/`AdminBootstrapProperties` avec **défaut `admin`/`admin123`** (cf. render.yaml).
- Swagger : `/v3/api-docs` + `/swagger-ui/**` en `permitAll()` (ouverts, y compris en prod).
- Limites upload Tomcat déjà posées (commit antérieur).

## §3. OBJECTIF STRICT
Durcir la posture sécurité sans changer les fonctionnalités ni le contrat REST.

DANS LE PÉRIMÈTRE (par priorité) :
1. **Secrets fail-fast** : refuser le démarrage en prod si `JWT_SECRET` est absent/laissé au défaut, et si le mot
   de passe admin est laissé au défaut (ou forcer un changement au 1er login). Aucun secret en dur dans le repo.
2. **Headers de sécurité** : `X-Content-Type-Options`, `X-Frame-Options`/frame-ancestors, `Referrer-Policy`,
   `HSTS` (prod), et une **CSP prudente**. ⚠️ La CSP ne doit PAS casser Swagger UI ni le frontend (tester).
3. **Exposition Swagger** : ouvert en dev, **restreint/désactivé en prod** (profil ou variable d'env). Décider et ADR.
4. **Revue rate-limit & quotas** : seuils cohérents, appliqués aux bonnes routes `/api/v1/**`, pas de contournement.
5. **CORS** : origines explicites (pas de `*` avec credentials), alignées sur la conf de déploiement.
6. **Validation des entrées** : confirmer limites de taille/type de fichier (anti-DoS upload) centralisées.

HORS PÉRIMÈTRE :
- Refonte de l'auth (OAuth/SSO) — pas maintenant.
- CI/scan de dépendances — étape 2.4.
- Le code métier PDF et le frontend (hors ajustement strictement nécessaire, ex. en-tête CSP côté nginx/back).

## §4. INVARIANTS
1. Aucune régression fonctionnelle ; contrat REST inchangé.
2. `mvn clean package` vert + `npm run build` vert.
3. Le mode **dev/local** doit rester simple à lancer (`docker compose up` fonctionne sans config secrète lourde) —
   le fail-fast ne s'applique qu'en profil prod.
4. **Validation locale (docker compose + UI + Swagger) avant tout commit/push** ; l'utilisateur valide d'abord.

## §5. DEFINITION OF DONE
- Démarrage prod impossible avec secrets par défaut ; dev inchangé.
- Headers de sécurité présents (vérifiables via `curl -I`), Swagger et UI fonctionnels.
- Politique d'exposition Swagger en prod décidée + ADR.
- Revue rate-limit/CORS/upload documentée (ce qui a changé et pourquoi).
- Builds verts.

## §6. GARDE-FOUS
- ❌ NE PAS poser une CSP qui casse Swagger/le frontend sans l'avoir testée.
- ❌ NE PAS committer de secret réel.
- ❌ NE PAS durcir au point de bloquer le `docker compose up` de dev.
- ❌ NE PAS pousser sans validation locale de l'utilisateur.

## §7. PROTOCOLE DE RETOUR
1. Liste des durcissements appliqués (secrets, headers, Swagger, rate-limit, CORS, upload).
2. Sortie `curl -I` montrant les headers (ou vérif statique si Docker indisponible).
3. ADR exposition Swagger + tout autre choix sécurité.
4. Résultat `mvn clean package` + `npm run build`.
5. Risques / dette restante pour 2.4 (CI/CD) — ex. scan de vulnérabilités des dépendances.
6. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 2.4 : …
