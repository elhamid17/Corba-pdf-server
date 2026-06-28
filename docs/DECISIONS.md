# Journal des décisions d'architecture (ADR)

> Registre des décisions structurantes du projet. Chaque ADR est immuable une fois acté ;
> une décision qu'on révise donne lieu à un nouvel ADR qui *supersede* l'ancien.
> Tenu par le Tech Lead. Dernière mise à jour : 2026-06-28.

---

## ADR-0001 — Évolution chirurgicale plutôt que réécriture
**Statut :** Acté
**Contexte :** ~22 000 LOC fonctionnelles et testées (41 handlers PDF + frontend React riche).
La tentation initiale était une « refonte totale, nouveau stack ».
**Décision :** On **n'effectue aucune réécriture from scratch**. On fait évoluer chirurgicalement :
on garde le frontend et les 41 handlers testés, on retire le mort (CORBA), on monte en grade par versions.
**Conséquences :** Risque minimal, valeur préservée. Interdiction permanente de « tout reprendre à zéro ».
**Justification :** Le mythe du rewrite est l'erreur d'ingénierie la plus documentée. Le bottleneck
du traitement PDF est la librairie (PDFBox), pas le langage.

## ADR-0002 — Suppression de CORBA, monolithe modulaire
**Statut :** Acté
**Contexte :** JacORB 3.9 (≈2017) relie deux process Java qui s'échangent des `byte[]`. Coût : IDL à
maintenir, sérialisation IIOP, fichier IOR partagé, 2e JVM, hack supervisord en prod.
**Décision :** Retirer entièrement CORBA. Les 41 handlers fusionnent dans la gateway comme couche
service **in-process**. Architecture cible = **monolithe modulaire** Spring Boot (un seul déployable).
**Conséquences :** Disparition de `corba-idl`, `corba-server`, JacORB, IOR, supervisord, mono-conteneur Render.
**Justification :** « Grade entreprise » ≠ microservices. Pour un solo, le monolithe modulaire est le
choix senior correct. Les microservices seraient un poison organisationnel à cette échelle.

## ADR-0003 — Modèle économique : open-core / self-host-first
**Statut :** Acté
**Contexte :** Ambition SaaS commercial, mais ressources = solo + budget ~0.
**Décision :** Produit ouvert et auto-hébergeable (modèle Stirling-PDF / Plausible / Supabase early).
L'utilisateur porte le compute en self-host. Monétisation ultérieure = tier hébergé + features pro.
**Justification :** Seul modèle SaaS viable sans capital. Aligné avec le positionnement local-first.

## ADR-0004 — Local-first séquencé
**Statut :** Acté
**Décision :** « Local-first » s'entend en deux temps :
- **Maintenant (A)** : zéro-rétention + auto-hébergeable, traitement serveur (on garde les 41 handlers).
- **V5 (B)** : traitement navigateur WASM pour les opérations sensibles uniquement.
**Justification :** A est compatible avec l'évolution chirurgicale et finançable à budget 0
(le compute est chez l'utilisateur). B est le moat ultime mais radical — on l'introduit quand le socle est prêt.

## ADR-0005 — Décisions de stack
**Statut :** Acté
**Garde :** React/Vite, Spring Boot/Java, PDFBox + handlers, MongoDB.
**Upgrade :** Java 17 → **21 LTS** (virtual threads, idéal I/O PDF + free tier) ; PDFBox 2.x → 3.x.
**Ajoute par versions :** queue de jobs, OpenTelemetry, OpenAPI, CI/CD.
**Rejette :** réécriture Rust/Go, microservices, Kafka, Kubernetes (avant V5).
**Justification :** Éviter la « langue de bois techno ». Optimiser la vitesse de livraison, pas la mode.

## ADR-0006 — Différenciation séquencée (pas simultanée)
**Statut :** Acté
**Contexte :** Les 4 axes (confidentialité, workflows, IA, verticale) choisis en même temps = 4 entreprises
de travail pour une personne. Impossible de front.
**Décision :** Séquencer, sans en abandonner aucun :
1. **Confidentialité / local-first** = colonne vertébrale + modèle éco.
2. **Workflows chaînés** = fer de lance produit.
3. **IA documentaire** = couche premium (BYO-key / modèle local pour respecter budget 0).
4. **Verticale métier** = packaging go-to-market, pas une décision d'architecture.

## ADR-0007 — Versioning d'API `/api/v1` : bascule franche (stratégie A)
**Statut :** Acté (V2, étape 2.2)
**Contexte :** Les routes étaient servies sans version (`/api/pdf`, `/api/auth`, `/api/admin`,
`/api/jobs`). Avant d'ouvrir l'API (OpenAPI/Swagger) et d'envisager des consommateurs externes,
il faut une stratégie de versioning pour pouvoir faire évoluer le contrat sans casser les clients.
Deux options étaient sur la table :
- **(A) Bascule franche** : `/api/v1` partout, frontend mis à jour en lockstep, pas d'alias `/api`.
- **(B) `/api/v1` canonique + ancien `/api` maintenu en alias déprécié** (migration douce).
**Décision :** On retient **(A) la bascule franche**. Toutes les routes applicatives passent sous
`/api/v1` (`/api/v1/pdf/...`, `/api/v1/auth/...`, `/api/v1/admin/...`, `/api/v1/jobs...`).
Le frontend est mis à jour dans le même commit (lockstep). Aucun alias `/api` n'est conservé.
**Mise en œuvre — source unique du préfixe (pas de chaînes dupliquées) :**
- Backend : `sn.ussein.gateway.web.ApiPaths` (constantes `V1`, `PDF`, `AUTH`, `ADMIN`, `JOBS`),
  référencées par les `@RequestMapping`, `SecurityConfig` et `RateLimitFilter`.
- Frontend : `frontend/src/api/routes.js` (`API_V1`, `PDF`, `AUTH`, `ADMIN`, `JOBS`),
  importées par `pdfApi/authApi/adminApi/jobsApi` et les pages qui appellent directement le client.
- nginx (`location /api/`) couvre déjà `/api/v1/` — inchangé. Les health-checks
  (docker-compose, `render.yaml`) et `VITE_API_URL` ont été alignés.
**Conséquences :** Contrat d'URL propre et versionnable ; un futur `v2` cohabitera sans ambiguïté.
Coût : un déploiement frontend+backend solidaire (acceptable : mono-repo, mono-déployable).
**Justification :** Le projet est local-first / self-host avec un seul frontend de référence ;
l'alias déprécié (B) ajouterait de la dette (double surface d'attaque, matrice de tests doublée)
pour un bénéfice nul à ce stade. La propreté l'emporte tant que la centralisation du préfixe
(source unique) rend la bascule atomique et sûre.

## ADR-0008 — Documentation d'API via OpenAPI/springdoc
**Statut :** Acté (V2, étape 2.2)
**Contexte :** Aucune documentation machine-lisible de l'API ; ADR-0005 prévoyait OpenAPI.
**Décision :** Ajout de `springdoc-openapi-starter-webmvc-ui` (ligne 2.3.x, compatible Spring Boot 3.2)
à `api-gateway`. Swagger UI exposé sur `/swagger-ui.html`, schéma JSON sur `/v3/api-docs`. Chaque
contrôleur porte un `@Tag` par domaine (Organisation, Conversion, Securite, Analyse, Generation, Ping,
Authentification, Administration, Jobs). Titre/description/version via un bean `OpenAPI`
(`OpenApiConfig`). Les routes doc/swagger sont **whitelistées** sans authentification dans `SecurityConfig`.
**Conséquences :** Ajout pur, sans rupture de comportement. Le durcissement de sécurité (faut-il
restreindre Swagger UI hors dev ?) relève de l'étape 2.3.
**Justification :** Prérequis à l'ouverture de l'API et à l'onboarding ; coût marginal, valeur immédiate.
